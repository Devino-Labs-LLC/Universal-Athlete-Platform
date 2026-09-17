-- V4 Slice B: Organization Stripe customer mapping, optional cadence /
-- provider-state timestamps, and a minimal provider-event receipt inbox
-- for verified Stripe webhook idempotency (ADR-044).
-- No payment credentials. No Apple/Google processing tables. Automatic tax is not enabled.
-- Full webhook payloads are not stored.

ALTER TABLE billing_subscriptions
	ADD COLUMN billing_cadence VARCHAR(10) NULL,
	ADD COLUMN provider_state_as_of DATETIME(6) NULL;

ALTER TABLE billing_subscriptions
	ADD CONSTRAINT ck_billing_subscriptions_cadence CHECK (
		billing_cadence IS NULL OR billing_cadence IN ('MONTHLY', 'ANNUAL')
	);

CREATE TABLE billing_organization_customers (
	organization_id BINARY(16) NOT NULL,
	provider VARCHAR(30) NOT NULL,
	provider_customer_ref VARCHAR(255) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (organization_id),
	CONSTRAINT ck_billing_org_customers_provider CHECK (
		provider IN ('STRIPE')
	),
	CONSTRAINT uk_billing_org_customers_provider_ref UNIQUE (provider, provider_customer_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE billing_provider_events (
	id BINARY(16) NOT NULL,
	provider VARCHAR(30) NOT NULL,
	provider_event_id VARCHAR(255) NOT NULL,
	event_type VARCHAR(120) NOT NULL,
	received_at DATETIME(6) NOT NULL,
	processed_at DATETIME(6) NULL,
	processing_status VARCHAR(20) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT ck_billing_provider_events_provider CHECK (
		provider IN ('STRIPE')
	),
	CONSTRAINT ck_billing_provider_events_status CHECK (
		processing_status IN ('RECEIVED', 'PROCESSED', 'IGNORED', 'FAILED')
	),
	CONSTRAINT uk_billing_provider_events_provider_event UNIQUE (provider, provider_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
