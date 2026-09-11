package com.devinolabs.uap.billing.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.billing.api.BillingSubjectType;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

/**
 * Billing-owned subscription persistence. Fields are local to this entity (not a
 * shared mapped superclass) to avoid New Code duplication across Modulith modules.
 */
@Entity
@Table(name = "billing_subscriptions")
class BillingSubscriptionJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

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

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean newlyPersisted = true;

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
			boolean newlyPersisted) {
		this.id = id;
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
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.version = version;
		this.newlyPersisted = newlyPersisted;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return newlyPersisted;
	}

	@PostLoad
	@PostPersist
	void markLoaded() {
		this.newlyPersisted = false;
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

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	long getVersion() {
		return version;
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
