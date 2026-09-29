package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.billing.application.AppleAppStoreBillingProvider.VerifiedPurchase;
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
 * Binds a server-validated Apple App Store purchase to the authenticated Account.
 * Fake tokens and foreign Account ownership fail closed.
 */
@Service
@ConditionalOnProperty(prefix = "uap.billing.apple", name = "enabled", havingValue = "true")
public class ApplePurchaseValidationService {

	private final AppleAppStoreBillingProvider appleProvider;
	private final SubscriptionRepository subscriptionRepository;
	private final IndividualSubscriptionConflictService conflictService;
	private final BillingAuditPort auditPort;
	private final Clock clock;

	public ApplePurchaseValidationService(
			AppleAppStoreBillingProvider appleProvider,
			SubscriptionRepository subscriptionRepository,
			IndividualSubscriptionConflictService conflictService,
			BillingAuditPort auditPort,
			Clock clock) {
		this.appleProvider = Objects.requireNonNull(appleProvider);
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.conflictService = Objects.requireNonNull(conflictService);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public SubscriptionResult validateOrRestore(UUID accountId, String signedTransactionInfo) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		if (signedTransactionInfo == null || signedTransactionInfo.isBlank()) {
			throw new InvalidApplePurchaseException("Signed transaction is required");
		}

		VerifiedPurchase purchase = appleProvider.validateSignedTransaction(signedTransactionInfo.trim());

		Subscription existing = subscriptionRepository
				.findByProviderAndProviderSubscriptionRef(
						BillingProvider.APPLE_APP_STORE, purchase.originalTransactionId())
				.orElse(null);
		if (existing != null) {
			requireMatchingAppAccountToken(purchase, accountId, false);
			requireOwnedByAccount(existing, accountId);
			return applySnapshot(existing, purchase, accountId, false);
		}

		requireMatchingAppAccountToken(purchase, accountId, true);
		conflictService.requireClearForNewIndividualPurchase(accountId, purchase.originalTransactionId());
		Subscription created = Subscription.startPendingIndividualApplePurchase(
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
		ProviderSubscriptionSnapshot snapshot = appleProvider.toSnapshot(purchase, accountId);
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
				|| subscription.provider() != BillingProvider.APPLE_APP_STORE) {
			throw new BillingAccountNotFoundException();
		}
	}

	private static void requireMatchingAppAccountToken(
			VerifiedPurchase purchase,
			UUID accountId,
			boolean required) {
		String token = purchase.appAccountToken();
		if (token == null) {
			if (required) {
				throw new InvalidApplePurchaseException("Apple appAccountToken is required to bind a purchase");
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
