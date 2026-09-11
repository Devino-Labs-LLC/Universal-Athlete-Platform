-- V4 Slice A: provider-neutral billing subscriptions (ADR-036–045).
-- No payment credentials. No provider Price/product IDs as primary keys.
-- Usage/seat enforcement and provider event inbox deferred to later slices.

CREATE TABLE billing_subscriptions (
	id BINARY(16) NOT NULL,
	subject_type VARCHAR(20) NOT NULL,
	subject_id BINARY(16) NOT NULL,
	provider VARCHAR(30) NOT NULL,
	plan_key VARCHAR(40) NOT NULL,
	lifecycle_state VARCHAR(30) NOT NULL,
	provider_customer_ref VARCHAR(255) NULL,
	provider_subscription_ref VARCHAR(255) NULL,
	trial_ends_at DATETIME(6) NULL,
	current_period_ends_at DATETIME(6) NULL,
	grace_ends_at DATETIME(6) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT ck_billing_subscriptions_subject_type CHECK (
		subject_type IN ('ACCOUNT', 'ORGANIZATION')
	),
	CONSTRAINT ck_billing_subscriptions_provider CHECK (
		provider IN ('STRIPE', 'APPLE_APP_STORE', 'GOOGLE_PLAY')
	),
	CONSTRAINT ck_billing_subscriptions_plan_key CHECK (
		plan_key IN ('ORG_BAND_25', 'ORG_BAND_75', 'ORG_BAND_250', 'INDIVIDUAL_PREMIUM')
	),
	CONSTRAINT ck_billing_subscriptions_lifecycle_state CHECK (
		lifecycle_state IN (
			'PENDING',
			'TRIALING',
			'ACTIVE',
			'PAST_DUE',
			'GRACE_PERIOD',
			'CANCEL_AT_PERIOD_END',
			'EXPIRED'
		)
	),
	CONSTRAINT uk_billing_subscriptions_provider_sub_ref UNIQUE (provider, provider_subscription_ref),
	INDEX idx_billing_subscriptions_subject (subject_type, subject_id),
	INDEX idx_billing_subscriptions_subject_state (subject_type, subject_id, lifecycle_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
