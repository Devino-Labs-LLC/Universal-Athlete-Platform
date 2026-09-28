package com.devinolabs.uap.billing.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.billing.domain.BillingProvider;

@Entity
@Table(name = "billing_account_customers")
class AccountBillingCustomerJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "account_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID accountId;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, updatable = false, length = 30)
	private BillingProvider provider;

	@Column(name = "provider_customer_ref", nullable = false, updatable = false, length = 255)
	private String providerCustomerRef;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AccountBillingCustomerJpaEntity() {
	}

	AccountBillingCustomerJpaEntity(
			UUID accountId,
			BillingProvider provider,
			String providerCustomerRef,
			Instant createdAt,
			Instant updatedAt) {
		this.accountId = accountId;
		this.provider = provider;
		this.providerCustomerRef = providerCustomerRef;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	@Override
	public UUID getId() {
		return accountId;
	}

	@Override
	public boolean isNew() {
		return true;
	}

	BillingProvider provider() {
		return provider;
	}

	String providerCustomerRef() {
		return providerCustomerRef;
	}

	Instant createdAt() {
		return createdAt;
	}

	Instant updatedAt() {
		return updatedAt;
	}

}
