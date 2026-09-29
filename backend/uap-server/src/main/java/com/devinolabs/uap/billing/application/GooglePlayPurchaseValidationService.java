package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.billing.application.GooglePlayBillingProvider.VerifiedPurchase;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.IndividualManagementChannel;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.entitlements.BillingSubjectType;

/**
 * Binds a server-validated Google Play purchase to the authenticated Account.
 * Fake tokens and foreign Account ownership fail closed.
 */
@Service
@ConditionalOnProperty(prefix = "uap.billing.google-play", name = "enabled", havingValue = "true")
public class GooglePlayPurchaseValidationService {

	private final GooglePlayBillingProvider googleProvider;
	private final SubscriptionRepository subscriptionRepository;
	private final IndividualSubscriptionConflictService conflictService;
	private final BillingAuditPort auditPort;
	private final Clock clock;

	public GooglePlayPurchaseValidationService(
			GooglePlayBillingProvider googleProvider,
			SubscriptionRepository subscriptionRepository,
			IndividualSubscriptionConflictService conflictService,
			BillingAuditPort auditPort,
			Clock clock) {
		this.googleProvider = Objects.requireNonNull(googleProvider);
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.conflictService = Objects.requireNonNull(conflictService);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public SubscriptionResult validateOrRestore(UUID accountId, String purchaseToken, String productId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		if (purchaseToken == null || purchaseToken.isBlank()) {
			throw new InvalidGooglePlayPurchaseException("Purchase token is required");
		}
		if (productId == null || productId.isBlank()) {
			throw new InvalidGooglePlayPurchaseException("Product id is required");
		}

		VerifiedPurchase purchase = googleProvider.validatePurchase(purchaseToken.trim(), productId.trim());

		Subscription existing = subscriptionRepository
				.findByProviderAndProviderSubscriptionRef(
						BillingProvider.GOOGLE_PLAY, purchase.purchaseToken())
				.orElse(null);
		if (existing != null) {
			requireMatchingObfuscatedAccountId(purchase, accountId, false);
			requireOwnedByAccount(existing, accountId);
			return applySnapshot(existing, purchase, accountId, false);
		}

		requireMatchingObfuscatedAccountId(purchase, accountId, true);
		conflictService.requireClearForNewIndividualPurchase(accountId, purchase.purchaseToken());
		Subscription created = Subscription.startPendingIndividualGooglePlayPurchase(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				purchase.planKey(),
				purchase.billingCadence(),
				clock);
		subscriptionRepository.save(created);
		auditPort.accountCheckoutInitiated(
				created.id().value(),
				accountId,
				accountId,
				purchase.planKey(),
				purchase.billingCadence());
		return applySnapshot(created, purchase, accountId, true);
	}

	private SubscriptionResult applySnapshot(
			Subscription subscription,
			VerifiedPurchase purchase,
			UUID accountId,
			boolean created) {
		ProviderSubscriptionSnapshot snapshot = googleProvider.toSnapshot(purchase, accountId);
		boolean changed = subscription.synchronizeProviderSnapshot(snapshot, clock);
		Subscription saved = changed ? subscriptionRepository.save(subscription) : subscription;
		if (changed || created) {
			auditPort.accountSubscriptionSynchronized(
					saved.id().value(),
					accountId,
					accountId,
					saved.lifecycleState());
		}
		return SubscriptionResult.from(saved);
	}

	private static void requireOwnedByAccount(Subscription subscription, UUID accountId) {
		if (subscription.subject().type() != BillingSubjectType.ACCOUNT
				|| !accountId.equals(subscription.subject().subjectId())
				|| subscription.provider() != BillingProvider.GOOGLE_PLAY) {
			throw new BillingAccountNotFoundException();
		}
	}

	private static void requireMatchingObfuscatedAccountId(
			VerifiedPurchase purchase,
			UUID accountId,
			boolean required) {
		String token = purchase.obfuscatedExternalAccountId();
		if (token == null) {
			if (required) {
				throw new InvalidGooglePlayPurchaseException(
						"Google Play obfuscatedExternalAccountId is required to bind a purchase");
			}
			return;
		}
		if (!accountId.toString().equalsIgnoreCase(token)) {
			throw new BillingAccountNotFoundException();
		}
	}

	public record SubscriptionResult(
			UUID subscriptionId,
			BillingProvider provider,
			IndividualManagementChannel managementChannel,
			CommercialPlanKey planKey,
			BillingCadence cadence,
			SubscriptionLifecycleState lifecycleState,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt) {

		static SubscriptionResult from(Subscription subscription) {
			return new SubscriptionResult(
					subscription.id().value(),
					subscription.provider(),
					IndividualManagementChannel.forProvider(subscription.provider()),
					subscription.planKey(),
					subscription.billingCadence(),
					subscription.lifecycleState(),
					subscription.trialEndsAt(),
					subscription.currentPeriodEndsAt(),
					subscription.graceEndsAt());
		}
	}

}
