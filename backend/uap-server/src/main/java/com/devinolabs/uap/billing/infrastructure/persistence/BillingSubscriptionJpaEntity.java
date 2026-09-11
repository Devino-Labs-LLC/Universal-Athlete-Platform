package com.devinolabs.uap.billing.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.devinolabs.uap.billing.api.BillingSubjectType;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

@Entity
@Table(name = "billing_subscriptions")
class BillingSubscriptionJpaEntity extends AbstractPersistableUuidJpaEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "subject_type", nullable = false, updatable = false, length = 20)
	private BillingSubjectType subjectType;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "subject_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID subjectId;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, updatable = false, length = 30)
	private BillingProvider provider;

	@Enumerated(EnumType.STRING)
	@Column(name = "plan_key", nullable = false, updatable = false, length = 40)
	private CommercialPlanKey planKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "lifecycle_state", nullable = false, length = 30)
	private SubscriptionLifecycleState lifecycleState;

	@Column(name = "provider_customer_ref", length = 255)
	private String providerCustomerRef;

	@Column(name = "provider_subscription_ref", length = 255)
	private String providerSubscriptionRef;

	@Column(name = "trial_ends_at")
	private Instant trialEndsAt;

	@Column(name = "current_period_ends_at")
	private Instant currentPeriodEndsAt;

	@Column(name = "grace_ends_at")
	private Instant graceEndsAt;

	protected BillingSubscriptionJpaEntity() {
	}

	BillingSubscriptionJpaEntity(
			UUID id,
			BillingSubjectType subjectType,
			UUID subjectId,
			BillingProvider provider,
			CommercialPlanKey planKey,
			SubscriptionLifecycleState lifecycleState,
			String providerCustomerRef,
			String providerSubscriptionRef,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		super(id, createdAt, updatedAt, version, isNew);
		this.subjectType = subjectType;
		this.subjectId = subjectId;
		this.provider = provider;
		this.planKey = planKey;
		this.lifecycleState = lifecycleState;
		this.providerCustomerRef = providerCustomerRef;
		this.providerSubscriptionRef = providerSubscriptionRef;
		this.trialEndsAt = trialEndsAt;
		this.currentPeriodEndsAt = currentPeriodEndsAt;
		this.graceEndsAt = graceEndsAt;
	}

	BillingSubjectType getSubjectType() {
		return subjectType;
	}

	UUID getSubjectId() {
		return subjectId;
	}

	BillingProvider getProvider() {
		return provider;
	}

	CommercialPlanKey getPlanKey() {
		return planKey;
	}

	SubscriptionLifecycleState getLifecycleState() {
		return lifecycleState;
	}

	String getProviderCustomerRef() {
		return providerCustomerRef;
	}

	String getProviderSubscriptionRef() {
		return providerSubscriptionRef;
	}

	Instant getTrialEndsAt() {
		return trialEndsAt;
	}

	Instant getCurrentPeriodEndsAt() {
		return currentPeriodEndsAt;
	}

	Instant getGraceEndsAt() {
		return graceEndsAt;
	}

	void applyDomainState(
			SubscriptionLifecycleState lifecycleState,
			String providerCustomerRef,
			String providerSubscriptionRef,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt,
			Instant updatedAt) {
		this.lifecycleState = lifecycleState;
		this.providerCustomerRef = providerCustomerRef;
		this.providerSubscriptionRef = providerSubscriptionRef;
		this.trialEndsAt = trialEndsAt;
		this.currentPeriodEndsAt = currentPeriodEndsAt;
		this.graceEndsAt = graceEndsAt;
		this.updatedAt = updatedAt;
	}

}
