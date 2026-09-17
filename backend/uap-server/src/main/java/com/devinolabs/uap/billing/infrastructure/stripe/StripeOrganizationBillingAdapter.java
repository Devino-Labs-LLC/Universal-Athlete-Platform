package com.devinolabs.uap.billing.infrastructure.stripe;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.stripe.StripeClient;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import com.devinolabs.uap.billing.application.BillingProviderUnavailableException;
import com.devinolabs.uap.billing.application.InvalidWebhookSignatureException;
import com.devinolabs.uap.billing.application.OrganizationBillingProvider;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;

@Component
@ConditionalOnProperty(prefix = "uap.billing.stripe", name = "enabled", havingValue = "true")
class StripeOrganizationBillingAdapter implements OrganizationBillingProvider {

	private static final String ORGANIZATION_ID = "uap_organization_id";
	private static final String SUBSCRIPTION_ID = "uap_subscription_id";
	private static final String PLAN_KEY = "uap_plan_key";
	private static final String CADENCE = "uap_billing_cadence";
	private static final long ORGANIZATION_TRIAL_DAYS = 14L;

	private final StripeClient stripeClient;
	private final StripeBillingProperties properties;
	private final Clock clock;

	StripeOrganizationBillingAdapter(
			StripeClient stripeClient,
			StripeBillingProperties properties,
			Clock clock) {
		this.stripeClient = Objects.requireNonNull(stripeClient);
		this.properties = Objects.requireNonNull(properties);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override
	public String createCustomer(UUID organizationId) {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		CustomerCreateParams params = CustomerCreateParams.builder()
				.setDescription("Athlete Readiness organization")
				.putMetadata(ORGANIZATION_ID, organizationId.toString())
				.build();
		try {
			Customer customer = stripeClient.v1().customers().create(
					params,
					idempotencyOptions("uap_org_customer_" + organizationId));
			requireSandbox(customer.getLivemode());
			return requireText(customer.getId(), "Stripe customer id");
		}
		catch (StripeException ex) {
			throw new BillingProviderUnavailableException(ex);
		}
	}

	@Override
	public CheckoutSession createCheckoutSession(
			UUID organizationId,
			UUID subscriptionId,
			String providerCustomerRef,
			CommercialPlanKey planKey,
			BillingCadence cadence) {
		SessionCreateParams params = checkoutParams(
				properties, organizationId, subscriptionId, providerCustomerRef, planKey, cadence);
		try {
			Session session = stripeClient.v1().checkout().sessions().create(
					params,
					idempotencyOptions("uap_org_checkout_" + subscriptionId));
			requireSandbox(session.getLivemode());
			return new CheckoutSession(session.getId(), session.getUrl());
		}
		catch (StripeException ex) {
			throw new BillingProviderUnavailableException(ex);
		}
	}

	static SessionCreateParams checkoutParams(
			StripeBillingProperties properties,
			UUID organizationId,
			UUID subscriptionId,
			String providerCustomerRef,
			CommercialPlanKey planKey,
			BillingCadence cadence) {
		String priceId = properties.priceId(planKey, cadence);
		SessionCreateParams.SubscriptionData subscriptionData = SessionCreateParams.SubscriptionData.builder()
				.setTrialPeriodDays(ORGANIZATION_TRIAL_DAYS)
				.putMetadata(ORGANIZATION_ID, organizationId.toString())
				.putMetadata(SUBSCRIPTION_ID, subscriptionId.toString())
				.putMetadata(PLAN_KEY, planKey.name())
				.putMetadata(CADENCE, cadence.name())
				.build();
		return SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
				.setCustomer(providerCustomerRef)
				.setClientReferenceId(subscriptionId.toString())
				.setSuccessUrl(properties.getSuccessUrl())
				.setCancelUrl(properties.getCancelUrl())
				.setPaymentMethodCollection(SessionCreateParams.PaymentMethodCollection.ALWAYS)
				.setIntegrationIdentifier(integrationIdentifier(subscriptionId))
				.putMetadata(ORGANIZATION_ID, organizationId.toString())
				.putMetadata(SUBSCRIPTION_ID, subscriptionId.toString())
				.putMetadata(PLAN_KEY, planKey.name())
				.putMetadata(CADENCE, cadence.name())
				.addLineItem(SessionCreateParams.LineItem.builder()
						.setPrice(priceId)
						.setQuantity(1L)
						.build())
				.setSubscriptionData(subscriptionData)
				.build();
	}

	@Override
	public ProviderSubscriptionSnapshot fetchCheckoutSubscription(
			UUID organizationId,
			UUID subscriptionId,
			String checkoutSessionId,
			String providerCustomerRef,
			CommercialPlanKey planKey,
			BillingCadence cadence) {
		try {
			Session session = stripeClient.v1().checkout().sessions().retrieve(requireText(
					checkoutSessionId, "checkoutSessionId"));
			requireSandbox(session.getLivemode());
			requireMatch(subscriptionId.toString(), session.getClientReferenceId(), "Checkout subscription");
			requireMatch(providerCustomerRef, session.getCustomer(), "Checkout customer");
			requireMetadata(session.getMetadata(), organizationId, subscriptionId, planKey, cadence);

			Subscription subscription = stripeClient.v1().subscriptions().retrieve(
					requireText(session.getSubscription(), "Stripe subscription id"));
			requireSandbox(subscription.getLivemode());
			requireMatch(providerCustomerRef, subscription.getCustomer(), "Subscription customer");
			requireMetadata(subscription.getMetadata(), organizationId, subscriptionId, planKey, cadence);
			SubscriptionItem item = requireSingleItem(subscription.getItems());
			properties.requireMatchingPrice(item.getPrice().getId(), planKey, cadence);

			return new ProviderSubscriptionSnapshot(
					subscription.getCustomer(),
					subscription.getId(),
					mapStatus(subscription.getStatus()),
					Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()),
					instant(subscription.getTrialEnd()),
					instant(item.getCurrentPeriodEnd()),
					Instant.now(clock));
		}
		catch (StripeException ex) {
			throw new BillingProviderUnavailableException(ex);
		}
	}

	@Override
	public VerifiedProviderEvent verifyWebhook(byte[] payload, String signatureHeader) {
		if (payload == null || payload.length == 0 || signatureHeader == null || signatureHeader.isBlank()) {
			throw new InvalidWebhookSignatureException();
		}
		try {
			Event event = Webhook.constructEvent(
					new String(payload, StandardCharsets.UTF_8),
					signatureHeader,
					properties.getWebhookSecret());
			return toVerifiedEvent(event);
		}
		catch (SignatureVerificationException ex) {
			throw new InvalidWebhookSignatureException();
		}
		catch (IllegalStateException ex) {
			throw new BillingProviderUnavailableException(ex);
		}
	}

	@Override
	public ProviderSubscriptionSnapshot fetchAuthoritativeSnapshot(VerifiedProviderEvent event) {
		Objects.requireNonNull(event, "event must not be null");
		try {
			Subscription subscription = retrieveSubscription(event);
			requireSandbox(subscription.getLivemode());
			Map<String, String> metadata = subscription.getMetadata() == null ? Map.of() : subscription.getMetadata();
			UUID organizationId = event.organizationId() != null
					? event.organizationId()
					: parseUuid(metadata.get(ORGANIZATION_ID));
			UUID subscriptionId = event.subscriptionId() != null
					? event.subscriptionId()
					: parseUuid(metadata.get(SUBSCRIPTION_ID));
			CommercialPlanKey planKey = CommercialPlanKey.valueOf(requireText(metadata.get(PLAN_KEY), "plan metadata"));
			BillingCadence cadence = BillingCadence.valueOf(requireText(metadata.get(CADENCE), "cadence metadata"));
			requireMetadata(metadata, organizationId, subscriptionId, planKey, cadence);
			SubscriptionItem item = requireSingleItem(subscription.getItems());
			properties.requireMatchingPrice(item.getPrice().getId(), planKey, cadence);
			return new ProviderSubscriptionSnapshot(
					subscription.getCustomer(),
					subscription.getId(),
					mapStatus(subscription.getStatus()),
					Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()),
					instant(subscription.getTrialEnd()),
					instant(item.getCurrentPeriodEnd()),
					event.createdAt());
		}
		catch (StripeException ex) {
			throw new BillingProviderUnavailableException(ex);
		}
	}

	private Subscription retrieveSubscription(VerifiedProviderEvent event) throws StripeException {
		if (event.checkoutSessionId() != null) {
			Session session = stripeClient.v1().checkout().sessions().retrieve(event.checkoutSessionId());
			requireSandbox(session.getLivemode());
			return stripeClient.v1().subscriptions().retrieve(
					requireText(session.getSubscription(), "Stripe subscription id"));
		}
		return stripeClient.v1().subscriptions().retrieve(
				requireText(event.providerSubscriptionRef(), "Stripe subscription id"));
	}

	static VerifiedProviderEvent toVerifiedEvent(Event event) {
		Objects.requireNonNull(event, "event must not be null");
		StripeObject object = requireEventObject(event);
		String checkoutSessionId = null;
		String providerSubscriptionRef = null;
		Map<String, String> metadata = Map.of();
		if (object instanceof Session session) {
			checkoutSessionId = session.getId();
			providerSubscriptionRef = session.getSubscription();
			metadata = session.getMetadata() == null ? Map.of() : session.getMetadata();
		}
		else if (object instanceof Subscription subscription) {
			providerSubscriptionRef = subscription.getId();
			metadata = subscription.getMetadata() == null ? Map.of() : subscription.getMetadata();
		}
		else if (object instanceof Invoice invoice) {
			providerSubscriptionRef = invoiceSubscriptionRef(invoice);
			metadata = invoiceMetadata(invoice);
		}
		Instant created = event.getCreated() == null ? Instant.EPOCH : Instant.ofEpochSecond(event.getCreated());
		return new VerifiedProviderEvent(
				event.getId(),
				event.getType(),
				Boolean.TRUE.equals(event.getLivemode()),
				created,
				checkoutSessionId,
				providerSubscriptionRef,
				parseUuid(metadata.get(ORGANIZATION_ID)),
				parseUuid(metadata.get(SUBSCRIPTION_ID)));
	}

	private static StripeObject requireEventObject(Event event) {
		var deserializer = event.getDataObjectDeserializer();
		StripeObject object = deserializer.getObject().orElse(null);
		if (object != null) {
			return object;
		}
		try {
			return deserializer.deserializeUnsafe();
		}
		catch (RuntimeException | EventDataObjectDeserializationException ex) {
			throw new IllegalStateException("Stripe event object could not be deserialized", ex);
		}
	}

	static Map<String, String> invoiceMetadata(Invoice invoice) {
		if (invoice.getParent() != null
				&& invoice.getParent().getSubscriptionDetails() != null
				&& invoice.getParent().getSubscriptionDetails().getMetadata() != null
				&& !invoice.getParent().getSubscriptionDetails().getMetadata().isEmpty()) {
			return invoice.getParent().getSubscriptionDetails().getMetadata();
		}
		return invoice.getMetadata() == null ? Map.of() : invoice.getMetadata();
	}

	private static String invoiceSubscriptionRef(Invoice invoice) {
		if (invoice.getParent() != null
				&& invoice.getParent().getSubscriptionDetails() != null
				&& invoice.getParent().getSubscriptionDetails().getSubscription() != null) {
			return invoice.getParent().getSubscriptionDetails().getSubscription();
		}
		return null;
	}

	private static UUID parseUuid(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(value.trim());
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	static ProviderCommercialStatus mapStatus(String status) {
		if (status == null) {
			return ProviderCommercialStatus.UNKNOWN;
		}
		return switch (status) {
			case "incomplete" -> ProviderCommercialStatus.PENDING;
			case "trialing" -> ProviderCommercialStatus.TRIALING;
			case "active" -> ProviderCommercialStatus.ACTIVE;
			case "past_due", "unpaid", "paused" -> ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED;
			case "canceled", "incomplete_expired" -> ProviderCommercialStatus.ENDED;
			default -> ProviderCommercialStatus.UNKNOWN;
		};
	}

	private static SubscriptionItem requireSingleItem(com.stripe.model.SubscriptionItemCollection items) {
		List<SubscriptionItem> data = items == null ? List.of() : items.getData();
		if (data == null || data.size() != 1 || data.getFirst().getPrice() == null
				|| !Long.valueOf(1L).equals(data.getFirst().getQuantity())) {
			throw new IllegalArgumentException("Stripe subscription must contain exactly one configured Price");
		}
		return data.getFirst();
	}

	private static void requireMetadata(
			Map<String, String> metadata,
			UUID organizationId,
			UUID subscriptionId,
			CommercialPlanKey planKey,
			BillingCadence cadence) {
		Map<String, String> values = metadata == null ? Map.of() : metadata;
		requireMatch(organizationId.toString(), values.get(ORGANIZATION_ID), "Organization metadata");
		requireMatch(subscriptionId.toString(), values.get(SUBSCRIPTION_ID), "Subscription metadata");
		requireMatch(planKey.name(), values.get(PLAN_KEY), "Plan metadata");
		requireMatch(cadence.name(), values.get(CADENCE), "Cadence metadata");
	}

	private static void requireMatch(String expected, String actual, String label) {
		if (!Objects.equals(expected, actual)) {
			throw new IllegalArgumentException(label + " does not match the requested Organization checkout");
		}
	}

	private static void requireSandbox(Boolean liveMode) {
		if (!Boolean.FALSE.equals(liveMode)) {
			throw new IllegalStateException("Stripe object is not confirmed as sandbox data");
		}
	}

	private static Instant instant(Long epochSeconds) {
		return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
	}

	private static RequestOptions idempotencyOptions(String key) {
		return RequestOptions.builder().setIdempotencyKey(key).build();
	}

	private static String integrationIdentifier(UUID subscriptionId) {
		long value = subscriptionId.getMostSignificantBits() ^ subscriptionId.getLeastSignificantBits();
		StringBuilder suffix = new StringBuilder(8);
		for (int index = 0; index < 8; index++) {
			suffix.append((char) ('a' + Math.floorMod(value, 26)));
			value /= 26;
		}
		return "athlete_readiness_" + suffix;
	}

	private static String requireText(String value, String label) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(label + " must not be blank");
		}
		return value.trim();
	}

}
