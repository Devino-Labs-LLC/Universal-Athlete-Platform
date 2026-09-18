package com.devinolabs.uap.billing.infrastructure.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stripe.StripeClient;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import com.devinolabs.uap.billing.application.BillingProviderUnavailableException;
import com.devinolabs.uap.billing.application.InvalidWebhookSignatureException;
import com.devinolabs.uap.billing.application.OrganizationBillingProvider;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;

@ExtendWith(MockitoExtension.class)
class StripeOrganizationBillingAdapterClientTests {

	private static final Instant NOW = Instant.parse("2026-09-13T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private StripeClient stripeClient;

	private StripeOrganizationBillingAdapter adapter;
	private UUID organizationId;
	private UUID subscriptionId;

	@BeforeEach
	void setUp() {
		adapter = new StripeOrganizationBillingAdapter(
				stripeClient, StripeBillingPropertiesTests.validProperties(), CLOCK);
		organizationId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		subscriptionId = UUID.fromString("11111111-2222-3333-4444-555555555555");
	}

	@Test
	void createCustomerRejectsLiveModeAndReturnsSandboxId() throws Exception {
		Customer live = new Customer();
		live.setId("cus_live");
		live.setLivemode(true);
		when(stripeClient.v1().customers().create(any(CustomerCreateParams.class), any())).thenReturn(live);
		assertThatThrownBy(() -> adapter.createCustomer(organizationId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("sandbox");

		Customer sandbox = new Customer();
		sandbox.setId("cus_sandbox");
		sandbox.setLivemode(false);
		when(stripeClient.v1().customers().create(any(CustomerCreateParams.class), any())).thenReturn(sandbox);
		assertThat(adapter.createCustomer(organizationId)).isEqualTo("cus_sandbox");
	}

	@Test
	void createCheckoutSessionReturnsHostedUrl() throws Exception {
		Session session = new Session();
		session.setId("cs_test_1");
		session.setUrl("https://checkout.stripe.test/cs_test_1");
		session.setLivemode(false);
		when(stripeClient.v1().checkout().sessions().create(any(SessionCreateParams.class), any()))
				.thenReturn(session);

		OrganizationBillingProvider.CheckoutSession result = adapter.createCheckoutSession(
				organizationId, subscriptionId, "cus_sandbox", CommercialPlanKey.ORG_BAND_75, BillingCadence.ANNUAL);

		assertThat(result.sessionId()).isEqualTo("cs_test_1");
		assertThat(result.checkoutUrl()).isEqualTo("https://checkout.stripe.test/cs_test_1");
	}

	@Test
	void fetchCheckoutSubscriptionMapsTrialingSnapshot() throws Exception {
		Session session = checkoutSession();
		when(stripeClient.v1().checkout().sessions().retrieve("cs_test_1")).thenReturn(session);
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription("trialing"));

		assertThat(adapter.fetchCheckoutSubscription(
				organizationId,
				subscriptionId,
				"cs_test_1",
				"cus_sandbox",
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL))
				.satisfies(snapshot -> {
					assertThat(snapshot.providerCustomerRef()).isEqualTo("cus_sandbox");
					assertThat(snapshot.providerSubscriptionRef()).isEqualTo("sub_test_1");
					assertThat(snapshot.status()).isEqualTo(ProviderCommercialStatus.TRIALING);
					assertThat(snapshot.cancelAtPeriodEnd()).isFalse();
					assertThat(snapshot.trialEndsAt()).isEqualTo(NOW.plusSeconds(14 * 24 * 60 * 60));
				});
	}

	@Test
	void fetchAuthoritativeSnapshotUsesSubscriptionRefWhenCheckoutSessionMissing() throws Exception {
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription("active"));
		OrganizationBillingProvider.VerifiedProviderEvent event = new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_1",
				"customer.subscription.updated",
				false,
				NOW.plusSeconds(30),
				null,
				"sub_test_1",
				organizationId,
				subscriptionId);

		assertThat(adapter.fetchAuthoritativeSnapshot(event).status()).isEqualTo(ProviderCommercialStatus.ACTIVE);
	}

	@Test
	void verifyWebhookRejectsMissingSignature() {
		assertThatThrownBy(() -> adapter.verifyWebhook("{}".getBytes(), " "))
				.isInstanceOf(InvalidWebhookSignatureException.class);
	}

	@Test
	void stripeExceptionsSurfaceAsProviderUnavailable() throws Exception {
		when(stripeClient.v1().customers().create(any(CustomerCreateParams.class), any()))
				.thenThrow(new com.stripe.exception.ApiException("down", "req", "code", 500, null));
		assertThatThrownBy(() -> adapter.createCustomer(organizationId))
				.isInstanceOf(BillingProviderUnavailableException.class);
	}

	@Test
	void createCheckoutSessionRejectsLiveMode() throws Exception {
		Session live = new Session();
		live.setId("cs_live");
		live.setUrl("https://checkout.stripe.com/cs_live");
		live.setLivemode(true);
		when(stripeClient.v1().checkout().sessions().create(any(SessionCreateParams.class), any())).thenReturn(live);

		assertThatThrownBy(() -> adapter.createCheckoutSession(
				organizationId, subscriptionId, "cus_sandbox", CommercialPlanKey.ORG_BAND_75, BillingCadence.ANNUAL))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("sandbox");
	}

	@Test
	void fetchCheckoutSubscriptionRejectsMetadataMismatchAndLiveMode() throws Exception {
		Session live = checkoutSession();
		live.setLivemode(true);
		when(stripeClient.v1().checkout().sessions().retrieve("cs_test_1")).thenReturn(live);
		assertThatThrownBy(() -> adapter.fetchCheckoutSubscription(
				organizationId,
				subscriptionId,
				"cs_test_1",
				"cus_sandbox",
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL))
				.isInstanceOf(IllegalStateException.class);

		Session mismatched = checkoutSession();
		mismatched.setCustomer("cus_other");
		when(stripeClient.v1().checkout().sessions().retrieve("cs_test_1")).thenReturn(mismatched);
		assertThatThrownBy(() -> adapter.fetchCheckoutSubscription(
				organizationId,
				subscriptionId,
				"cs_test_1",
				"cus_sandbox",
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("customer");
	}

	@Test
	void fetchAuthoritativeSnapshotUsesCheckoutSessionWhenPresent() throws Exception {
		when(stripeClient.v1().checkout().sessions().retrieve("cs_test_1")).thenReturn(checkoutSession());
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription("trialing"));
		OrganizationBillingProvider.VerifiedProviderEvent event = new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_checkout",
				"checkout.session.completed",
				false,
				NOW.plusSeconds(10),
				"cs_test_1",
				"sub_test_1",
				organizationId,
				subscriptionId);

		assertThat(adapter.fetchAuthoritativeSnapshot(event).providerSubscriptionRef()).isEqualTo("sub_test_1");
	}

	@Test
	void verifyWebhookRejectsInvalidSignatureAndUnsignedPayload() {
		assertThatThrownBy(() -> adapter.verifyWebhook("{}".getBytes(), "t=1,v1=deadbeef"))
				.isInstanceOf(InvalidWebhookSignatureException.class);
		assertThatThrownBy(() -> adapter.verifyWebhook(new byte[0], "sig"))
				.isInstanceOf(InvalidWebhookSignatureException.class);
	}

	@Test
	void verifyWebhookAcceptsSignedSubscriptionEvent() throws Exception {
		String payload = """
				{"id":"evt_test_1","object":"event","api_version":"2026-08-26.dahlia","created":1757764800,"type":"customer.subscription.updated","livemode":false,"data":{"object":{"id":"sub_test_1","object":"subscription","customer":"cus_sandbox","status":"active","livemode":false,"metadata":{"uap_organization_id":"%s","uap_subscription_id":"%s"}}}}
				""".formatted(organizationId, subscriptionId).strip();
		String header = StripeOrganizationBillingAdapterTests.signedWebhookHeader(
				payload, StripeBillingPropertiesTests.validProperties().getWebhookSecret());

		assertThat(adapter.verifyWebhook(payload.getBytes(), header)).satisfies(event -> {
			assertThat(event.eventId()).isEqualTo("evt_test_1");
			assertThat(event.eventType()).isEqualTo("customer.subscription.updated");
			assertThat(event.providerSubscriptionRef()).isEqualTo("sub_test_1");
			assertThat(event.organizationId()).isEqualTo(organizationId);
			assertThat(event.subscriptionId()).isEqualTo(subscriptionId);
			assertThat(event.liveMode()).isFalse();
		});
	}

	@Test
	void verifyWebhookSignedEventWithoutObjectIsProviderUnavailable() throws Exception {
		String payload = "{\"id\":\"evt_empty\"}";
		String header = StripeOrganizationBillingAdapterTests.signedWebhookHeader(
				payload, StripeBillingPropertiesTests.validProperties().getWebhookSecret());
		assertThatThrownBy(() -> adapter.verifyWebhook(payload.getBytes(), header))
				.isInstanceOf(BillingProviderUnavailableException.class);
	}

	private Session checkoutSession() {
		Session session = new Session();
		session.setId("cs_test_1");
		session.setLivemode(false);
		session.setCustomer("cus_sandbox");
		session.setSubscription("sub_test_1");
		session.setClientReferenceId(subscriptionId.toString());
		session.setMetadata(uapMetadata());
		return session;
	}

	private Subscription subscription(String status) {
		Price price = new Price();
		price.setId("price_75_annual");
		SubscriptionItem item = new SubscriptionItem();
		item.setPrice(price);
		item.setQuantity(1L);
		item.setCurrentPeriodEnd(NOW.plusSeconds(30L * 24 * 60 * 60).getEpochSecond());
		SubscriptionItemCollection items = new SubscriptionItemCollection();
		items.setData(List.of(item));
		Subscription subscription = new Subscription();
		subscription.setId("sub_test_1");
		subscription.setLivemode(false);
		subscription.setCustomer("cus_sandbox");
		subscription.setStatus(status);
		subscription.setCancelAtPeriodEnd(false);
		subscription.setTrialEnd(NOW.plusSeconds(14L * 24 * 60 * 60).getEpochSecond());
		subscription.setMetadata(uapMetadata());
		subscription.setItems(items);
		return subscription;
	}

	private Map<String, String> uapMetadata() {
		return Map.of(
				"uap_organization_id", organizationId.toString(),
				"uap_subscription_id", subscriptionId.toString(),
				"uap_plan_key", CommercialPlanKey.ORG_BAND_75.name(),
				"uap_billing_cadence", BillingCadence.ANNUAL.name());
	}

}
