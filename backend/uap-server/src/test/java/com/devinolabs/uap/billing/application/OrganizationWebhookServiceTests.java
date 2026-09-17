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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
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
				"sub_" + eventId,
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
				asOf);
	}

}
