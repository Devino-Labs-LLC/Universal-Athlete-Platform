-- Identity accounts foundation.
-- Stores authentication credentials only; no organization or athlete data.

CREATE TABLE accounts (
	id BINARY(16) NOT NULL,
	email VARCHAR(320) NOT NULL,
	password_hash VARCHAR(100) NOT NULL,
	status VARCHAR(32) NOT NULL,
	failed_login_attempts INT NOT NULL DEFAULT 0,
	locked_until DATETIME(6) NULL,
	email_verified_at DATETIME(6) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uk_accounts_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
