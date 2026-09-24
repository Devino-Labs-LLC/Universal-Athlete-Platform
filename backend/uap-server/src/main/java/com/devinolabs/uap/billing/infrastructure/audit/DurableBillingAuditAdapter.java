package com.devinolabs.uap.billing.infrastructure.audit;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.billing.application.BillingAuditPort;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

@Component
class DurableBillingAuditAdapter implements BillingAuditPort {

	private final SecurityAuditWriter auditWriter;

	DurableBillingAuditAdapter(SecurityAuditWriter auditWriter) {
		this.auditWriter = Objects.requireNonNull(auditWriter);
	}

	@Override
	public void checkoutInitiated(
			UUID subscriptionId,
			UUID organizationId,
			UUID actorAccountId,
			CommercialPlanKey planKey,
			BillingCadence cadence) {
		append(
				"BILLING_CHECKOUT_INITIATED",
				subscriptionId,
				organizationId,
				actorAccountId,
				"{\"planKey\":\"" + planKey.name() + "\",\"cadence\":\"" + cadence.name() + "\"}");
	}

	@Override
	public void subscriptionSynchronized(
			UUID subscriptionId,
			UUID organizationId,
			UUID actorAccountId,
			SubscriptionLifecycleState lifecycleState) {
		append(
				"BILLING_SUBSCRIPTION_SYNCHRONIZED",
				subscriptionId,
				organizationId,
				actorAccountId,
				"{\"lifecycleState\":\"" + lifecycleState.name() + "\"}");
	}

	@Override
	public void subscriptionActivated(
			UUID subscriptionId,
			UUID organizationId,
			SubscriptionLifecycleState lifecycleState) {
		append(
				"BILLING_SUBSCRIPTION_ACTIVATED",
				subscriptionId,
				organizationId,
				null,
				"{\"lifecycleState\":\"" + lifecycleState.name() + "\"}");
	}

	@Override
	public void planChanged(
			UUID subscriptionId,
			UUID organizationId,
			UUID actorAccountId,
			CommercialPlanKey fromPlan,
			CommercialPlanKey toPlan,
			BillingCadence cadence) {
		append(
				"BILLING_PLAN_CHANGED",
				subscriptionId,
				organizationId,
				actorAccountId,
				"{\"fromPlan\":\"" + fromPlan.name() + "\",\"toPlan\":\"" + toPlan.name()
						+ "\",\"cadence\":\"" + cadence.name() + "\"}");
	}

	@Override
	public void cancelRequested(UUID subscriptionId, UUID organizationId, UUID actorAccountId) {
		append(
				"BILLING_CANCEL_REQUESTED",
				subscriptionId,
				organizationId,
				actorAccountId,
				"{}");
	}

	@Override
	public void subscriptionReactivated(UUID subscriptionId, UUID organizationId, UUID actorAccountId) {
		append(
				"BILLING_SUBSCRIPTION_REACTIVATED",
				subscriptionId,
				organizationId,
				actorAccountId,
				"{}");
	}

	@Override
	public void subscriptionEnded(UUID subscriptionId, UUID organizationId) {
		append(
				"BILLING_SUBSCRIPTION_ENDED",
				subscriptionId,
				organizationId,
				null,
				"{}");
	}

	private void append(
			String eventType,
			UUID subscriptionId,
			UUID organizationId,
			UUID actorAccountId,
			String metadataJson) {
		auditWriter.append(SecurityAuditRecord.of(
				eventType,
				actorAccountId,
				null,
				null,
				organizationId,
				null,
				"BILLING_SUBSCRIPTION",
				subscriptionId,
				metadataJson));
	}

}
