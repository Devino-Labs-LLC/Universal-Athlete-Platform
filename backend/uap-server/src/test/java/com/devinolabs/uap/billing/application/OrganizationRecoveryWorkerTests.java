package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.billing.application.OrganizationBillingProvider.PendingCheckoutInspection;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingEndReason;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.OrganizationBillingCustomer;
import com.devinolabs.uap.billing.domain.ProviderCollectionState;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.entitlements.BillingSubjectType;

class OrganizationRecoveryWorkerTests {

	private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private MemorySubscriptions subscriptions;
	private RecordingProvider provider;
	private RecordingAudit audit;
	private OrganizationRecoveryWorker worker;

	@BeforeEach
	void setUp() {
		subscriptions = new MemorySubscriptions();
		provider = new RecordingProvider();
		audit = new RecordingAudit();
		OrganizationBillingCustomerRepository customers = mock(OrganizationBillingCustomerRepository.class);
		when(customers.findByOrganizationId(orgId())).thenReturn(
				Optional.of(OrganizationBillingCustomer.stripe(orgId(), "cus_pending", NOW)));
		worker = new OrganizationRecoveryWorker(
				subscriptions,
				customers,
				provider,
				audit,
				CLOCK,
				new TransactionTemplate() {
					@Override
					public <T> T execute(TransactionCallback<T> action) {
						return action.doInTransaction(new SimpleTransactionStatus());
					}
				});
	}

	@Test
	void dueGraceRecoversWhenProviderIsActiveWithoutCancelling() {
		Subscription subscription = dueGrace();
		provider.next = activeSnapshot(NOW.plusSeconds(5));

		worker.reconcile();

		assertThat(provider.cancelCalls).isZero();
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.ACTIVE);
		assertThat(subscription(subscription).graceEndsAt()).isNull();
		assertThat(audit.recovered).isEqualTo(1);
	}

	@Test
	void dueGraceExpiresATerminalProviderWithoutASecondCancel() {
		Subscription subscription = dueGrace();
		provider.next = endedSnapshot(NOW.plusSeconds(5));

		worker.reconcile();

		assertThat(provider.cancelCalls).isZero();
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		assertThat(audit.endedReason).isEqualTo(BillingEndReason.NONPAYMENT);
		assertThat(audit.ended).isEqualTo(1);
	}

	@Test
	void dueGraceTerminatesTheSameSubscriptionWithTheGraceKey() {
		Subscription subscription = dueGrace();
		provider.next = attention(ProviderCollectionState.PAST_DUE, NOW.plusSeconds(5));
		provider.afterCancel = endedSnapshot(NOW.plusSeconds(6));

		worker.reconcile();

		assertThat(provider.cancelCalls).isEqualTo(1);
		assertThat(provider.lastKey).isEqualTo(OrganizationRecoveryWorker.graceExpireKey(
				subscription.id().value(), subscription.graceEndsAt()));
		assertThat(provider.lastRef).isEqualTo("sub_grace");
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		assertThat(audit.ended).isEqualTo(1);
	}

	@Test
	void providerOutageLeavesElapsedGraceWithoutAnEndAudit() {
		Subscription subscription = dueGrace();
		provider.unavailable = true;

		worker.reconcile();

		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
		assertThat(subscription(subscription).isCommerciallyEntitledAt(NOW)).isFalse();
		assertThat(audit.ended).isZero();
	}

	@Test
	void activeObservedBeforeCancelIsNotCancelled() {
		Subscription subscription = dueGrace();
		provider.next = attention(ProviderCollectionState.UNPAID, NOW.plusSeconds(4));
		provider.recoverBeforeCancel = true;

		worker.reconcile();

		assertThat(provider.cancelCalls).isZero();
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.ACTIVE);
		assertThat(audit.recovered).isEqualTo(1);
	}

	@Test
	void secondTickPersistsExpiredAfterTheLocalWriteWasLost() {
		Subscription subscription = dueGrace();
		provider.next = attention(ProviderCollectionState.PAST_DUE, NOW.plusSeconds(4));
		provider.afterCancel = endedSnapshot(NOW.plusSeconds(6));
		subscriptions.failNextExpiredSave = true;

		worker.reconcile();
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
		assertThat(provider.cancelCalls).isEqualTo(1);

		provider.next = endedSnapshot(NOW.plusSeconds(7));
		worker.reconcile();
		assertThat(provider.cancelCalls).isEqualTo(1);
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		assertThat(audit.ended).isEqualTo(1);
	}

	@Test
	void twoWorkersShareOneIdempotencyKeyAndOneEndAudit() throws Exception {
		Subscription subscription = dueGrace();
		provider.next = attention(ProviderCollectionState.PAST_DUE, NOW.plusSeconds(4));
		provider.afterCancel = endedSnapshot(NOW.plusSeconds(8));
		provider.cancelEntered = new CountDownLatch(2);
		provider.releaseCancel = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(2);
		AtomicInteger failures = new AtomicInteger();
		Runnable run = () -> {
			try {
				worker.reconcile();
			}
			catch (RuntimeException ex) {
				failures.incrementAndGet();
			}
			finally {
				done.countDown();
			}
		};
		Thread first = new Thread(run);
		Thread second = new Thread(run);
		first.start();
		second.start();
		assertThat(provider.cancelEntered.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
		provider.releaseCancel.countDown();
		assertThat(done.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

		assertThat(failures).hasValue(0);
		assertThat(provider.keys).containsOnly(OrganizationRecoveryWorker.graceExpireKey(
				subscription.id().value(), subscription.graceEndsAt()));
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		assertThat(audit.ended).isEqualTo(1);
	}

	@Test
	void exceptionalPastDueDoesNotInventGrace() {
		Subscription subscription = pastDue();
		provider.next = attention(ProviderCollectionState.PAST_DUE, NOW.plusSeconds(3));

		worker.reconcile();

		assertThat(provider.cancelCalls).isZero();
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.PAST_DUE);
		assertThat(subscription(subscription).graceEndsAt()).isNull();
	}

	@Test
	void exceptionalUnpaidPastDueTerminatesWithTheStableKey() {
		Subscription subscription = pastDue();
		provider.next = attention(ProviderCollectionState.UNPAID, NOW.plusSeconds(3));
		provider.afterCancel = endedSnapshot(NOW.plusSeconds(4));

		worker.reconcile();

		assertThat(provider.lastKey).isEqualTo(OrganizationRecoveryWorker.pastDueTerminateKey(subscription.id().value()));
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		assertThat(audit.endedReason).isEqualTo(BillingEndReason.NONPAYMENT);
	}

	@Test
	void elapsedCancelExpiresOnlyWhenTheProviderIsTerminal() {
		Subscription subscription = cancelAtPeriodEnd();
		provider.next = activeSnapshot(NOW.plusSeconds(2));
		worker.reconcile();
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.CANCEL_AT_PERIOD_END);

		provider.next = endedSnapshot(NOW.plusSeconds(3));
		worker.reconcile();
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		assertThat(audit.endedReason).isEqualTo(BillingEndReason.UNSPECIFIED);
	}

	@Test
	void stalePendingFollowsProviderCheckoutTruth() {
		Subscription pending = pending();
		provider.checkout = PendingCheckoutInspection.of(PendingCheckoutInspection.Outcome.OPEN);
		worker.reconcile();
		assertThat(subscription(pending).lifecycleState()).isEqualTo(SubscriptionLifecycleState.PENDING);

		provider.checkout = PendingCheckoutInspection.of(PendingCheckoutInspection.Outcome.EXPIRED);
		worker.reconcile();
		assertThat(subscription(pending).lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		assertThat(audit.endedReason).isEqualTo(BillingEndReason.UNSPECIFIED);
	}

	@Test
	void pausedPastDueTerminatesWithTheStableKey() {
		Subscription subscription = pastDue();
		provider.next = attention(ProviderCollectionState.PAUSED, NOW.plusSeconds(3));
		provider.afterCancel = endedSnapshot(NOW.plusSeconds(4));

		worker.reconcile();

		assertThat(provider.lastKey).isEqualTo(OrganizationRecoveryWorker.pastDueTerminateKey(subscription.id().value()));
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		assertThat(audit.endedReason).isEqualTo(BillingEndReason.NONPAYMENT);
	}

	@Test
	void completedPendingCheckoutSynchronizesInsteadOfExpiring() {
		Subscription pending = pending();
		provider.checkout = new PendingCheckoutInspection(
				PendingCheckoutInspection.Outcome.COMPLETE, activeSnapshot(NOW.plusSeconds(2)));

		worker.reconcile();

		assertThat(subscription(pending).lifecycleState()).isEqualTo(SubscriptionLifecycleState.ACTIVE);
		assertThat(audit.ended).isZero();
	}

	@Test
	void dueGraceWithoutAProviderSubscriptionIsNotTerminated() {
		Subscription subscription = dueGrace();
		subscription.attachProviderReferences("cus_grace", null, CLOCK);
		subscriptions.save(subscription);

		worker.reconcile();

		assertThat(provider.cancelCalls).isZero();
		assertThat(subscription(subscription).lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
	}

	@Test
	void pastDueAndElapsedCancelOutagesLeaveTheRows() {
		Subscription unpaid = pastDue();
		Subscription scheduled = cancelAtPeriodEnd();
		provider.unavailable = true;

		worker.reconcile();

		assertThat(subscription(unpaid).lifecycleState()).isEqualTo(SubscriptionLifecycleState.PAST_DUE);
		assertThat(subscription(scheduled).lifecycleState()).isEqualTo(SubscriptionLifecycleState.CANCEL_AT_PERIOD_END);
		assertThat(audit.ended).isZero();
	}

	@Test
	void pendingWithoutAStoredCustomerStaysPending() {
		Subscription pending = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.generate(),
				BillingSubject.organization(UUID.randomUUID()),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				Clock.fixed(NOW.minusSeconds(26 * 60 * 60), ZoneOffset.UTC));
		subscriptions.save(pending);

		worker.reconcile();

		assertThat(subscription(pending).lifecycleState()).isEqualTo(SubscriptionLifecycleState.PENDING);
	}

	@Test
	void unavailablePendingLookupLeavesTheRowPending() {
		Subscription pending = pending();
		provider.unavailable = true;

		worker.reconcile();

		assertThat(subscription(pending).lifecycleState()).isEqualTo(SubscriptionLifecycleState.PENDING);
		assertThat(audit.ended).isZero();
	}

	private Subscription dueGrace() {
		Subscription subscription = activeSubscription();
		subscription.establishGrace(attention(ProviderCollectionState.PAST_DUE, NOW.minusSeconds(10)), NOW.minusSeconds(8 * 24 * 60 * 60), CLOCK);
		return subscriptions.save(subscription);
	}

	private Subscription pastDue() {
		Subscription subscription = activeSubscription();
		subscription.recordExceptionalPaymentAttention(attention(ProviderCollectionState.UNPAID, NOW.minusSeconds(5)), CLOCK);
		return subscriptions.save(subscription);
	}

	private Subscription cancelAtPeriodEnd() {
		Subscription subscription = activeSubscription();
		subscription.scheduleCancelAtPeriodEnd(NOW.minusSeconds(60), Clock.fixed(NOW.minusSeconds(120), ZoneOffset.UTC));
		return subscriptions.save(subscription);
	}

	private Subscription pending() {
		Subscription subscription = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.generate(),
				BillingSubject.organization(orgId()),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				Clock.fixed(NOW.minusSeconds(26 * 60 * 60), ZoneOffset.UTC));
		return subscriptions.save(subscription);
	}

	private Subscription activeSubscription() {
		Clock created = Clock.fixed(NOW.minusSeconds(10 * 24 * 60 * 60), ZoneOffset.UTC);
		Subscription subscription = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.generate(),
				BillingSubject.organization(orgId()),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				created);
		subscription.activate(NOW.plusSeconds(30 * 24 * 60 * 60), created);
		subscription.attachProviderReferences("cus_grace", "sub_grace", created);
		return subscription;
	}

	private Subscription subscription(Subscription original) {
		return subscriptions.findById(original.id()).orElseThrow();
	}

	private static UUID orgId() {
		return UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
	}

	private static ProviderSubscriptionSnapshot attention(ProviderCollectionState collection, Instant asOf) {
		return new ProviderSubscriptionSnapshot(
				"cus_grace", "sub_grace", ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED, false,
				null, NOW.plusSeconds(30 * 24 * 60 * 60), CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY,
				asOf, collection);
	}

	private static ProviderSubscriptionSnapshot activeSnapshot(Instant asOf) {
		return new ProviderSubscriptionSnapshot(
				"cus_grace", "sub_grace", ProviderCommercialStatus.ACTIVE, false,
				null, NOW.plusSeconds(40 * 24 * 60 * 60), CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY, asOf);
	}

	private static ProviderSubscriptionSnapshot endedSnapshot(Instant asOf) {
		return new ProviderSubscriptionSnapshot(
				"cus_grace", "sub_grace", ProviderCommercialStatus.ENDED, false,
				null, NOW.plusSeconds(30 * 24 * 60 * 60), CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY, asOf);
	}

	private static final class MemorySubscriptions implements SubscriptionRepository {

		private final java.util.Map<SubscriptionId, Subscription> store = new java.util.concurrent.ConcurrentHashMap<>();
		private boolean failNextExpiredSave;

		@Override
		public synchronized Subscription save(Subscription subscription) {
			if (failNextExpiredSave && subscription.lifecycleState() == SubscriptionLifecycleState.EXPIRED) {
				failNextExpiredSave = false;
				throw new ObjectOptimisticLockingFailureException(Subscription.class, subscription.id().value());
			}
			Subscription existing = store.get(subscription.id());
			if (existing != null
					&& existing.lifecycleState() == SubscriptionLifecycleState.EXPIRED
					&& subscription.lifecycleState() == SubscriptionLifecycleState.EXPIRED
					&& existing != subscription) {
				throw new ObjectOptimisticLockingFailureException(Subscription.class, subscription.id().value());
			}
			store.put(subscription.id(), subscription);
			return subscription;
		}

		@Override
		public synchronized Optional<Subscription> findById(SubscriptionId id) {
			Subscription stored = store.get(id);
			return stored == null ? Optional.empty() : Optional.of(copy(stored));
		}

		private static Subscription copy(Subscription source) {
			return Subscription.rehydrate(
					source.id(),
					source.subject(),
					source.provider(),
					source.planKey(),
					source.billingCadence(),
					source.lifecycleState(),
					source.providerCustomerRef(),
					source.providerSubscriptionRef(),
					source.trialEndsAt(),
					source.currentPeriodEndsAt(),
					source.graceEndsAt(),
					source.providerStateAsOf(),
					source.createdAt(),
					source.updatedAt(),
					source.version());
		}

		@Override
		public List<Subscription> findBySubject(BillingSubjectType subjectType, UUID subjectId) {
			return List.of();
		}

		@Override
		public Optional<Subscription> findByProviderAndProviderSubscriptionRef(
				BillingProvider provider, String providerSubscriptionRef) {
			return Optional.empty();
		}

		@Override
		public List<Subscription> findDueGrace(Instant now, int limit) {
			return store.values().stream()
					.filter(subscription -> subscription.lifecycleState() == SubscriptionLifecycleState.GRACE_PERIOD
							&& subscription.graceEndsAt() != null
							&& !subscription.graceEndsAt().isAfter(now))
					.limit(limit)
					.toList();
		}

		@Override
		public List<Subscription> findPastDue(int limit) {
			return store.values().stream()
					.filter(subscription -> subscription.lifecycleState() == SubscriptionLifecycleState.PAST_DUE)
					.limit(limit)
					.toList();
		}

		@Override
		public List<Subscription> findStalePending(Instant createdAtOrBefore, int limit) {
			return store.values().stream()
					.filter(subscription -> subscription.lifecycleState() == SubscriptionLifecycleState.PENDING
							&& !subscription.createdAt().isAfter(createdAtOrBefore))
					.limit(limit)
					.toList();
		}

		@Override
		public List<Subscription> findElapsedCancelAtPeriodEnd(Instant now, int limit) {
			return store.values().stream()
					.filter(subscription -> subscription.lifecycleState() == SubscriptionLifecycleState.CANCEL_AT_PERIOD_END
							&& subscription.currentPeriodEndsAt() != null
							&& !subscription.currentPeriodEndsAt().isAfter(now))
					.limit(limit)
					.toList();
		}
	}

	private static final class RecordingProvider implements OrganizationBillingProvider {

		private ProviderSubscriptionSnapshot next = activeSnapshot(NOW);
		private ProviderSubscriptionSnapshot afterCancel = endedSnapshot(NOW);
		private PendingCheckoutInspection checkout = PendingCheckoutInspection.of(PendingCheckoutInspection.Outcome.NOT_FOUND);
		private boolean unavailable;
		private boolean recoverBeforeCancel;
		private int cancelCalls;
		private String lastKey;
		private String lastRef;
		private final java.util.Set<String> keys = java.util.concurrent.ConcurrentHashMap.newKeySet();
		private CountDownLatch cancelEntered;
		private CountDownLatch releaseCancel;

		@Override
		public ProviderSubscriptionSnapshot fetchSubscription(String providerSubscriptionRef) {
			if (unavailable) {
				throw new BillingProviderUnavailableException(new IllegalStateException("down"));
			}
			return next;
		}

		@Override
		public ProviderSubscriptionSnapshot terminateForNonpayment(
				UUID subscriptionId, String providerSubscriptionRef, String idempotencyKey) {
			if (cancelEntered != null) {
				cancelEntered.countDown();
				try {
					if (!releaseCancel.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
						throw new IllegalStateException("cancel was not released");
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(ex);
				}
			}
			keys.add(idempotencyKey);
			lastKey = idempotencyKey;
			lastRef = providerSubscriptionRef;
			if (recoverBeforeCancel || next.status() == ProviderCommercialStatus.ACTIVE) {
				return activeSnapshot(NOW.plusSeconds(9));
			}
			cancelCalls++;
			next = afterCancel;
			return afterCancel;
		}

		@Override
		public PendingCheckoutInspection lookupPendingCheckout(String providerCustomerRef, UUID subscriptionId) {
			if (unavailable) {
				throw new BillingProviderUnavailableException(new IllegalStateException("down"));
			}
			return checkout;
		}

		@Override
		public String createCustomer(UUID organizationId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public CheckoutSession createCheckoutSession(
				UUID organizationId, UUID subscriptionId, String providerCustomerRef,
				CommercialPlanKey planKey, BillingCadence cadence) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ProviderSubscriptionSnapshot fetchCheckoutSubscription(
				UUID organizationId, UUID subscriptionId, String checkoutSessionId, String providerCustomerRef,
				CommercialPlanKey planKey, BillingCadence cadence) {
			throw new UnsupportedOperationException();
		}

		@Override
		public VerifiedProviderEvent verifyWebhook(byte[] payload, String signatureHeader) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ProviderSubscriptionSnapshot fetchAuthoritativeSnapshot(VerifiedProviderEvent event) {
			throw new UnsupportedOperationException();
		}

		@Override
		public PortalSession createPortalSession(UUID organizationId, String providerCustomerRef) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ProviderSubscriptionSnapshot changeSubscriptionPlan(
				UUID subscriptionId, String providerSubscriptionRef, CommercialPlanKey targetPlanKey,
				BillingCadence targetCadence, UUID requestId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ProviderSubscriptionSnapshot restoreSubscriptionPlan(
				UUID subscriptionId, String providerSubscriptionRef, CommercialPlanKey planKey,
				BillingCadence cadence, String operationToken) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ProviderSubscriptionSnapshot scheduleCancelAtPeriodEnd(
				UUID subscriptionId, String providerSubscriptionRef, UUID requestId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ProviderSubscriptionSnapshot reactivateSubscription(
				UUID subscriptionId, String providerSubscriptionRef, UUID requestId) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class RecordingAudit implements BillingAuditPort {

		private int recovered;
		private int ended;
		private BillingEndReason endedReason;

		@Override
		public void paymentRecovered(UUID subscriptionId, UUID organizationId) {
			recovered++;
		}

		@Override
		public void subscriptionEnded(UUID subscriptionId, UUID organizationId, BillingEndReason reason) {
			ended++;
			endedReason = reason;
		}

		@Override
		public void checkoutInitiated(UUID subscriptionId, UUID organizationId, UUID actorAccountId, CommercialPlanKey planKey, BillingCadence cadence) {
		}

		@Override
		public void subscriptionSynchronized(UUID subscriptionId, UUID organizationId, UUID actorAccountId, SubscriptionLifecycleState lifecycleState) {
		}

		@Override
		public void subscriptionActivated(UUID subscriptionId, UUID organizationId, SubscriptionLifecycleState lifecycleState) {
		}

		@Override
		public void planChanged(UUID subscriptionId, UUID organizationId, UUID actorAccountId, CommercialPlanKey fromPlan, CommercialPlanKey toPlan, BillingCadence cadence) {
		}

		@Override
		public void cancelRequested(UUID subscriptionId, UUID organizationId, UUID actorAccountId) {
		}

		@Override
		public void subscriptionReactivated(UUID subscriptionId, UUID organizationId, UUID actorAccountId) {
		}

		@Override
		public void graceStarted(UUID subscriptionId, UUID organizationId) {
		}

		@Override
		public void accountCheckoutInitiated(
				UUID subscriptionId,
				UUID accountId,
				UUID actorAccountId,
				CommercialPlanKey planKey,
				BillingCadence cadence) {
		}

		@Override
		public void accountSubscriptionSynchronized(
				UUID subscriptionId,
				UUID accountId,
				UUID actorAccountId,
				SubscriptionLifecycleState lifecycleState) {
		}
	}
}
