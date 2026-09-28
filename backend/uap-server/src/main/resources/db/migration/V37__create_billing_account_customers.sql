-- V4 Slice G1: Account Stripe customer mapping for Individual Premium checkout.
-- Widens the provider-event inbox CHECK so later Apple/Google slices can claim events.
-- No payment credentials. No automatic tax. No payload storage.

CREATE TABLE billing_account_customers (
	account_id BINARY(16) NOT NULL,
	provider VARCHAR(30) NOT NULL,
	provider_customer_ref VARCHAR(255) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (account_id),
	CONSTRAINT ck_billing_account_customers_provider CHECK (
		provider IN ('STRIPE')
	),
	CONSTRAINT uk_billing_account_customers_provider_ref UNIQUE (provider, provider_customer_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE billing_provider_events
	DROP CHECK ck_billing_provider_events_provider;

ALTER TABLE billing_provider_events
	ADD CONSTRAINT ck_billing_provider_events_provider CHECK (
		provider IN ('STRIPE', 'APPLE_APP_STORE', 'GOOGLE_PLAY')
	);
