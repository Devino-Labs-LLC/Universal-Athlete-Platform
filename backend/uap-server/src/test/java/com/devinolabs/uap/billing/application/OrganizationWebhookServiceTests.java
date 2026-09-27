package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.BillingPolicies;
import com.devinolabs.uap.billing.domain.ProviderCollectionState;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

class OrganizationWebhookServiceTests {

	private static final Instant NOW = Instant.parse("2026-09-13T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private OrganizationBillingProvider billingProvider;
	private ProviderEventInbox eventInbox;
	private SubscriptionRepository subscriptionRepository;
	private BillingAuditPort auditPort;
	private OrganizationWebhookService service;

	@BeforeEach
	void setUp() {
		billingProvider = mock(OrganizationBillingProvider.class);
		eventInbox = mock(ProviderEventInbox.class);
		subscriptionRepository = mock(SubscriptionRepository.class);
		auditPort = mock(BillingAuditPort.class);
		TransactionTemplate transactions = new TransactionTemplate() {
			@Override
			public <T> T execute(TransactionCallback<T> action) {
				return action.doInTransaction(new SimpleTransactionStatus());
			}

			@Override
			public void executeWithoutResult(java.util.function.Consumer<TransactionStatus> action) {
				action.accept(new SimpleTransactionStatus());
			}
		};
		service = new OrganizationWebhookService(
				billingProvider,
				eventInbox,
				subscriptionRepository,
				auditPort,
				mock(com.devinolabs.uap.organization.api.OrganizationMembershipPort.class),
				mock(OrganizationSubscriptionManagementService.class),
				CLOCK,
				transactions);
	}

	@Test
	void invalidSignatureNeverTouchesInboxOrSubscriptions() {
		when(billingProvider.verifyWebhook(any(), any())).thenThrow(new InvalidWebhookSignatureException());

		assertThatThrownBy(() -> service.handle(new byte[] { 1 }, "bad"))
				.isInstanceOf(InvalidWebhookSignatureException.class);
		verify(eventInbox, never()).tryBegin(any(), any(), any(), any());
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void duplicateEventIdIsIgnoredAfterFirstClaim() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		OrganizationBillingProvider.VerifiedProviderEvent event = event(
				"evt_dup", "checkout.session.completed", organizationId, subscriptionId, NOW.plusSeconds(5));
		when(billingProvider.verifyWebhook(any(), any())).thenReturn(event);
		when(eventInbox.tryBegin(eq(BillingProvider.STRIPE), eq("evt_dup"), any(), any()))
				.thenReturn(Optional.empty());

		service.handle("{}".getBytes(), "sig");

		verify(billingProvider, never()).fetchAuthoritativeSnapshot(any());
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void newerSnapshotIsAppliedAndOlderReplayDoesNotRegress() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription pending = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.organization(organizationId),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				CLOCK);
		ProviderEventReceipt receipt = new ProviderEventReceipt(
				UUID.randomUUID(),
				BillingProvider.STRIPE,
				"evt_new",
				"customer.subscription.updated",
				NOW,
				null,
				ProviderEventProcessingStatus.RECEIVED);
		when(billingProvider.verifyWebhook(any(), any())).thenReturn(
				event("evt_new", "customer.subscription.updated", organizationId, subscriptionId, NOW.plusSeconds(30)));
		when(eventInbox.tryBegin(any(), eq("evt_new"), any(), any())).thenReturn(Optional.of(receipt));
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(pending));
		when(billingProvider.fetchAuthoritativeSnapshot(any())).thenReturn(snapshot(
				ProviderCommercialStatus.TRIALING, NOW.plusSeconds(30)));
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.handle("{}".getBytes(), "sig");

		assertThat(pending.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);
		verify(auditPort).subscriptionActivated(subscriptionId, organizationId, SubscriptionLifecycleState.TRIALING);
		verify(eventInbox).complete(receipt.id(), ProviderEventProcessingStatus.PROCESSED, NOW);

		assertThat(pending.synchronizeProviderSnapshot(
				snapshot(ProviderCommercialStatus.PENDING, NOW.plusSeconds(1)), CLOCK)).isFalse();
		assertThat(pending.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);
	}

	@Test
	void concurrentSubscriptionWritesAreRetriedThenCompleted() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription pending = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.organization(organizationId),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				CLOCK);
		ProviderEventReceipt receipt = new ProviderEventReceipt(
				UUID.randomUUID(),
				BillingProvider.STRIPE,
				"evt_race",
				"invoice.paid",
				NOW,
				null,
				ProviderEventProcessingStatus.RECEIVED);
		when(billingProvider.verifyWebhook(any(), any())).thenReturn(
				event("evt_race", "invoice.paid", organizationId, subscriptionId, NOW.plusSeconds(30)));
		when(eventInbox.tryBegin(any(), eq("evt_race"), any(), any())).thenReturn(Optional.of(receipt));
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(pending));
		when(billingProvider.fetchAuthoritativeSnapshot(any())).thenReturn(snapshot(
				ProviderCommercialStatus.TRIALING, NOW.plusSeconds(30)));
		when(subscriptionRepository.save(any()))
				.thenThrow(new ObjectOptimisticLockingFailureException(Subscription.class, subscriptionId))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.handle("{}".getBytes(), "sig");

		assertThat(pending.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);
		verify(subscriptionRepository, times(2)).findById(SubscriptionId.of(subscriptionId));
		verify(eventInbox).complete(receipt.id(), ProviderEventProcessingStatus.PROCESSED, NOW);
	}

	@Test
	void subscriptionUpdatedWithoutIdentityMetadataAppliesByProviderReference() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription pending = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.organization(organizationId),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				CLOCK);
		ProviderEventReceipt receipt = new ProviderEventReceipt(
				UUID.randomUUID(),
				BillingProvider.STRIPE,
				"evt_meta",
				"customer.subscription.updated",
				NOW,
				null,
				ProviderEventProcessingStatus.RECEIVED);
		when(billingProvider.verifyWebhook(any(), any())).thenReturn(new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_meta",
				"customer.subscription.updated",
				false,
				NOW.plusSeconds(30),
				null,
				"sub_test",
				null,
				null));
		when(eventInbox.tryBegin(any(), eq("evt_meta"), any(), any())).thenReturn(Optional.of(receipt));
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(BillingProvider.STRIPE, "sub_test"))
				.thenReturn(Optional.of(pending));
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(pending));
		when(billingProvider.fetchAuthoritativeSnapshot(any())).thenReturn(snapshot(
				ProviderCommercialStatus.TRIALING, NOW.plusSeconds(30)));
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.handle("{}".getBytes(), "sig");

		assertThat(pending.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);
		assertThat(pending.providerSubscriptionRef()).isEqualTo("sub_test");
		verify(eventInbox).complete(receipt.id(), ProviderEventProcessingStatus.PROCESSED, NOW);
	}

	@Test
	void unknownProviderReferenceWithoutIdentityIsIgnored() {
		ProviderEventReceipt receipt = new ProviderEventReceipt(
				UUID.randomUUID(),
				BillingProvider.STRIPE,
				"evt_unknown_ref",
				"customer.subscription.updated",
				NOW,
				null,
				ProviderEventProcessingStatus.RECEIVED);
		when(billingProvider.verifyWebhook(any(), any())).thenReturn(new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_unknown_ref",
				"customer.subscription.updated",
				false,
				NOW.plusSeconds(5),
				null,
				"sub_missing",
				null,
				null));
		when(eventInbox.tryBegin(any(), eq("evt_unknown_ref"), any(), any())).thenReturn(Optional.of(receipt));
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(BillingProvider.STRIPE, "sub_missing"))
				.thenReturn(Optional.empty());

		service.handle("{}".getBytes(), "sig");

		verify(billingProvider, never()).fetchAuthoritativeSnapshot(any());
		verify(eventInbox).complete(receipt.id(), ProviderEventProcessingStatus.IGNORED, NOW);
	}

	@Test
	void providerReferenceMismatchIsIgnored() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription bound = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.organization(organizationId),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				CLOCK);
		assertThat(bound.synchronizeProviderSnapshot(
				snapshot(ProviderCommercialStatus.ACTIVE, NOW.plusSeconds(5)), CLOCK)).isTrue();
		ProviderEventReceipt receipt = new ProviderEventReceipt(
				UUID.randomUUID(),
				BillingProvider.STRIPE,
				"evt_mismatch",
				"customer.subscription.updated",
				NOW,
				null,
				ProviderEventProcessingStatus.RECEIVED);
		when(billingProvider.verifyWebhook(any(), any())).thenReturn(new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_mismatch",
				"customer.subscription.updated",
				false,
				NOW.plusSeconds(20),
				null,
				"sub_other",
				organizationId,
				subscriptionId));
		when(eventInbox.tryBegin(any(), eq("evt_mismatch"), any(), any())).thenReturn(Optional.of(receipt));
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(bound));

		service.handle("{}".getBytes(), "sig");

		assertThat(bound.providerSubscriptionRef()).isEqualTo("sub_test");
		verify(billingProvider, never()).fetchAuthoritativeSnapshot(any());
		verify(eventInbox).complete(receipt.id(), ProviderEventProcessingStatus.IGNORED, NOW);
	}

	@Test
	void liveModeAndUnknownTypesAreDurablyIgnored() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		ProviderEventReceipt receipt = new ProviderEventReceipt(
				UUID.randomUUID(),
				BillingProvider.STRIPE,
				"evt_live",
				"checkout.session.completed",
				NOW,
				null,
				ProviderEventProcessingStatus.RECEIVED);
		OrganizationBillingProvider.VerifiedProviderEvent live = new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_live",
				"checkout.session.completed",
				true,
				NOW,
				"cs_live",
				"sub_live",
				organizationId,
				subscriptionId);
		when(billingProvider.verifyWebhook(any(), any())).thenReturn(live);
		when(eventInbox.tryBegin(any(), eq("evt_live"), any(), any())).thenReturn(Optional.of(receipt));

		service.handle("{}".getBytes(), "sig");

		verify(subscriptionRepository, never()).findById(any());
		verify(eventInbox).complete(receipt.id(), ProviderEventProcessingStatus.IGNORED, NOW);
		verify(billingProvider, times(1)).verifyWebhook(any(), any());
	}

	@Test
	void paymentFailureOnActiveEntersGraceWithoutPassingThroughPastDue() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription active = active(organizationId, subscriptionId);
		Instant failure = NOW.plusSeconds(5);
		stub("evt_fail", "invoice.payment_failed", organizationId, subscriptionId, failure, active,
				attention(ProviderCollectionState.PAST_DUE, failure));

		service.handle("{}".getBytes(), "sig");

		assertThat(active.lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
		assertThat(active.graceEndsAt()).isEqualTo(BillingPolicies.graceDeadline(failure));
		assertThat(active.isCommerciallyEntitledAt(NOW)).isTrue();
		verify(auditPort).graceStarted(subscriptionId, organizationId);
		verify(auditPort, never()).paymentRecovered(any(), any());
	}

	@Test
	void trialConversionFailureKeepsTrialEnd() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription trialing = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.organization(organizationId),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				CLOCK);
		trialing.beginOrganizationTrial(CLOCK);
		trialing.attachProviderReferences("cus_test", "sub_test", CLOCK);
		Instant trialEnd = trialing.trialEndsAt();
		Instant failure = NOW.plusSeconds(8);
		stub("evt_trial_fail", "invoice.payment_failed", organizationId, subscriptionId, failure, trialing,
				attention(ProviderCollectionState.PAST_DUE, failure));

		service.handle("{}".getBytes(), "sig");

		assertThat(trialing.lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
		assertThat(trialing.trialEndsAt()).isEqualTo(trialEnd);
		verify(auditPort).graceStarted(subscriptionId, organizationId);
	}

	@Test
	void olderFailureTightensGraceAndStaleFailureAfterRecoveryDoesNot() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription active = active(organizationId, subscriptionId);
		Instant later = NOW.plusSeconds(20);
		stub("evt_updated", "customer.subscription.updated", organizationId, subscriptionId, later, active,
				attention(ProviderCollectionState.PAST_DUE, later));
		service.handle("{}".getBytes(), "sig");
		assertThat(active.graceEndsAt()).isEqualTo(BillingPolicies.graceDeadline(later));

		Instant earlier = NOW.plusSeconds(5);
		stub("evt_older", "invoice.payment_failed", organizationId, subscriptionId, earlier, active,
				attention(ProviderCollectionState.PAST_DUE, earlier));
		service.handle("{}".getBytes(), "sig");
		assertThat(active.graceEndsAt()).isEqualTo(BillingPolicies.graceDeadline(earlier));
		verify(auditPort, times(1)).graceStarted(subscriptionId, organizationId);

		Instant recoveredAt = NOW.plusSeconds(30);
		stub("evt_paid", "invoice.paid", organizationId, subscriptionId, recoveredAt, active,
				snapshot(ProviderCommercialStatus.ACTIVE, recoveredAt));
		service.handle("{}".getBytes(), "sig");
		assertThat(active.lifecycleState()).isEqualTo(SubscriptionLifecycleState.ACTIVE);
		assertThat(active.graceEndsAt()).isNull();
		verify(auditPort).paymentRecovered(subscriptionId, organizationId);
		verify(auditPort, never()).subscriptionActivated(subscriptionId, organizationId, SubscriptionLifecycleState.ACTIVE);

		stub("evt_stale", "invoice.payment_failed", organizationId, subscriptionId, earlier, active,
				attention(ProviderCollectionState.PAST_DUE, earlier.plusSeconds(1)));
		service.handle("{}".getBytes(), "sig");
		assertThat(active.lifecycleState()).isEqualTo(SubscriptionLifecycleState.ACTIVE);
		assertThat(active.graceEndsAt()).isNull();
		verify(auditPort, times(1)).graceStarted(subscriptionId, organizationId);
	}

	@Test
	void paymentActionRequiredEntersGraceLikeAFailedInvoice() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription active = active(organizationId, subscriptionId);
		Instant failure = NOW.plusSeconds(6);
		stub("evt_action", "invoice.payment_action_required", organizationId, subscriptionId, failure, active,
				attention(ProviderCollectionState.PAST_DUE, failure));

		service.handle("{}".getBytes(), "sig");

		assertThat(active.lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
		assertThat(active.graceEndsAt()).isEqualTo(BillingPolicies.graceDeadline(failure));
		verify(auditPort).graceStarted(subscriptionId, organizationId);
	}

	@Test
	void laterFailureDoesNotExtendAnOpenGraceDeadline() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription active = active(organizationId, subscriptionId);
		Instant first = NOW.plusSeconds(4);
		stub("evt_first", "customer.subscription.updated", organizationId, subscriptionId, first, active,
				attention(ProviderCollectionState.PAST_DUE, first));
		service.handle("{}".getBytes(), "sig");
		Instant deadline = active.graceEndsAt();

		Instant later = NOW.plusSeconds(90);
		stub("evt_later", "invoice.payment_failed", organizationId, subscriptionId, later, active,
				attention(ProviderCollectionState.PAST_DUE, later));
		service.handle("{}".getBytes(), "sig");

		assertThat(active.graceEndsAt()).isEqualTo(deadline);
		verify(auditPort, times(1)).graceStarted(subscriptionId, organizationId);
	}

	@Test
	void stalePaidEventDoesNotClearGrace() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription active = active(organizationId, subscriptionId);
		Instant failure = NOW.plusSeconds(12);
		stub("evt_fail_paid", "invoice.payment_failed", organizationId, subscriptionId, failure, active,
				attention(ProviderCollectionState.PAST_DUE, failure));
		service.handle("{}".getBytes(), "sig");

		stub("evt_stale_paid", "invoice.paid", organizationId, subscriptionId, failure.minusSeconds(30), active,
				snapshot(ProviderCommercialStatus.ACTIVE, failure.minusSeconds(30)));
		service.handle("{}".getBytes(), "sig");

		assertThat(active.lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
		assertThat(active.graceEndsAt()).isEqualTo(BillingPolicies.graceDeadline(failure));
		verify(auditPort, never()).paymentRecovered(any(), any());
	}

	@Test
	void providerDeletionDuringOpenGraceDoesNotExpireEarly() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription active = active(organizationId, subscriptionId);
		Instant failure = NOW.plusSeconds(2);
		stub("evt_open", "invoice.payment_failed", organizationId, subscriptionId, failure, active,
				attention(ProviderCollectionState.PAST_DUE, failure));
		service.handle("{}".getBytes(), "sig");
		Instant deadline = active.graceEndsAt();

		Instant deletedAt = failure.plusSeconds(3 * 24 * 60 * 60);
		stub("evt_deleted", "customer.subscription.deleted", organizationId, subscriptionId, deletedAt, active,
				snapshot(ProviderCommercialStatus.ENDED, deletedAt));
		service.handle("{}".getBytes(), "sig");

		assertThat(active.lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
		assertThat(active.graceEndsAt()).isEqualTo(deadline);
		assertThat(active.isCommerciallyEntitledAt(deletedAt)).isTrue();
		verify(auditPort, never()).subscriptionEnded(any(), any(), any());
	}

	@Test
	void unpaidAndPausedDoNotStartGrace() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription active = active(organizationId, subscriptionId);
		stub("evt_unpaid", "invoice.payment_failed", organizationId, subscriptionId, NOW.plusSeconds(4), active,
				attention(ProviderCollectionState.UNPAID, NOW.plusSeconds(4)));
		service.handle("{}".getBytes(), "sig");
		assertThat(active.lifecycleState()).isEqualTo(SubscriptionLifecycleState.PAST_DUE);
		assertThat(active.graceEndsAt()).isNull();
		assertThat(active.isCommerciallyEntitledAt(NOW)).isFalse();
		verify(auditPort, never()).graceStarted(any(), any());

		Subscription paused = active(UUID.randomUUID(), UUID.randomUUID());
		stub("evt_paused", "customer.subscription.updated", paused.subject().subjectId(), paused.id().value(),
				NOW.plusSeconds(5), paused, attention(ProviderCollectionState.PAUSED, NOW.plusSeconds(5)));
		service.handle("{}".getBytes(), "sig");
		assertThat(paused.lifecycleState()).isEqualTo(SubscriptionLifecycleState.PAST_DUE);
		assertThat(paused.graceEndsAt()).isNull();
	}

	@Test
	void unpaidAfterGraceKeepsTheOriginalDeadline() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription active = active(organizationId, subscriptionId);
		Instant failure = NOW.plusSeconds(3);
		stub("evt_grace", "invoice.payment_failed", organizationId, subscriptionId, failure, active,
				attention(ProviderCollectionState.PAST_DUE, failure));
		service.handle("{}".getBytes(), "sig");
		Instant deadline = active.graceEndsAt();

		stub("evt_unpaid_later", "invoice.payment_failed", organizationId, subscriptionId, failure.plusSeconds(60), active,
				attention(ProviderCollectionState.UNPAID, failure.plusSeconds(60)));
		service.handle("{}".getBytes(), "sig");

		assertThat(active.lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
		assertThat(active.graceEndsAt()).isEqualTo(deadline);
		verify(auditPort, times(1)).graceStarted(subscriptionId, organizationId);
	}

	@Test
	void expiredCheckoutDoesNotExpireAnEntitledRelationship() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription pending = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.organization(organizationId),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				CLOCK);
		stub("evt_complete", "checkout.session.expired", organizationId, subscriptionId, NOW.plusSeconds(4), pending,
				snapshot(ProviderCommercialStatus.TRIALING, NOW.plusSeconds(4)));
		service.handle("{}".getBytes(), "sig");
		assertThat(pending.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);

		UUID trialingOrg = UUID.randomUUID();
		UUID trialingId = UUID.randomUUID();
		Subscription trialing = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.of(trialingId),
				BillingSubject.organization(trialingOrg),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				CLOCK);
		trialing.beginOrganizationTrial(CLOCK);
		OrganizationBillingProvider.VerifiedProviderEvent expired = new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_trial_expired", "checkout.session.expired", false, NOW.plusSeconds(6), "cs_trial", null,
				trialing.subject().subjectId(), trialing.id().value());
		when(billingProvider.verifyWebhook(any(), any())).thenReturn(expired);
		when(eventInbox.tryBegin(any(), eq("evt_trial_expired"), any(), any())).thenReturn(Optional.of(new ProviderEventReceipt(
				UUID.randomUUID(), BillingProvider.STRIPE, "evt_trial_expired", "checkout.session.expired", NOW, null,
				ProviderEventProcessingStatus.RECEIVED)));
		when(subscriptionRepository.findById(trialing.id())).thenReturn(Optional.of(trialing));
		service.handle("{}".getBytes(), "sig");
		assertThat(trialing.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);
		verify(billingProvider, never()).fetchAuthoritativeSnapshot(org.mockito.ArgumentMatchers.argThat(
				event -> "evt_trial_expired".equals(event.eventId())));
	}

	@Test
	void expiredCheckoutWithoutProviderSubscriptionExpiresPending() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription pending = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.organization(organizationId),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				CLOCK);
		OrganizationBillingProvider.VerifiedProviderEvent expired = new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_expired",
				"checkout.session.expired",
				false,
				NOW.plusSeconds(3),
				"cs_expired",
				null,
				organizationId,
				subscriptionId);
		when(billingProvider.verifyWebhook(any(), any())).thenReturn(expired);
		when(eventInbox.tryBegin(any(), eq("evt_expired"), any(), any())).thenReturn(Optional.of(new ProviderEventReceipt(
				UUID.randomUUID(), BillingProvider.STRIPE, "evt_expired", "checkout.session.expired", NOW, null,
				ProviderEventProcessingStatus.RECEIVED)));
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(pending));
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.handle("{}".getBytes(), "sig");

		assertThat(pending.lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		verify(billingProvider, never()).fetchAuthoritativeSnapshot(any());
		verify(auditPort).subscriptionEnded(subscriptionId, organizationId, com.devinolabs.uap.billing.domain.BillingEndReason.UNSPECIFIED);

		service.handle("{}".getBytes(), "sig");
		verify(auditPort, times(1)).subscriptionEnded(subscriptionId, organizationId, com.devinolabs.uap.billing.domain.BillingEndReason.UNSPECIFIED);
	}

	private void stub(
			String eventId,
			String type,
			UUID organizationId,
			UUID subscriptionId,
			Instant createdAt,
			Subscription subscription,
			ProviderSubscriptionSnapshot snapshot) {
		when(billingProvider.verifyWebhook(any(), any())).thenReturn(
				event(eventId, type, organizationId, subscriptionId, createdAt));
		when(eventInbox.tryBegin(any(), eq(eventId), any(), any())).thenReturn(Optional.of(new ProviderEventReceipt(
				UUID.randomUUID(), BillingProvider.STRIPE, eventId, type, NOW, null, ProviderEventProcessingStatus.RECEIVED)));
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(subscription));
		when(billingProvider.fetchAuthoritativeSnapshot(any())).thenReturn(snapshot);
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private static Subscription active(UUID organizationId, UUID subscriptionId) {
		Subscription subscription = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.organization(organizationId),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				CLOCK);
		subscription.activate(NOW.plusSeconds(30 * 24 * 60 * 60), CLOCK);
		subscription.attachProviderReferences("cus_test", "sub_test", CLOCK);
		return subscription;
	}

	private static ProviderSubscriptionSnapshot attention(ProviderCollectionState collection, Instant asOf) {
		return new ProviderSubscriptionSnapshot(
				"cus_test",
				"sub_test",
				ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED,
				false,
				null,
				NOW.plusSeconds(30 * 24 * 60 * 60),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				asOf,
				collection);
	}

	private static OrganizationBillingProvider.VerifiedProviderEvent event(
			String eventId,
			String type,
			UUID organizationId,
			UUID subscriptionId,
			Instant createdAt) {
		return new OrganizationBillingProvider.VerifiedProviderEvent(
				eventId,
				type,
				false,
				createdAt,
				"cs_" + eventId,
				"sub_test",
				organizationId,
				subscriptionId);
	}

	private static ProviderSubscriptionSnapshot snapshot(ProviderCommercialStatus status, Instant asOf) {
		return new ProviderSubscriptionSnapshot(
				"cus_test",
				"sub_test",
				status,
				false,
				status == ProviderCommercialStatus.TRIALING ? NOW.plusSeconds(14 * 24 * 60 * 60) : null,
				NOW.plusSeconds(30 * 24 * 60 * 60),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				asOf);
	}

}
