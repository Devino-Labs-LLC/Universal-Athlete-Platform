-- Email verification tokens for identity registration.
-- Persists token digests only; raw tokens are never stored.

CREATE TABLE email_verification_tokens (
	id BINARY(16) NOT NULL,
	account_id BINARY(16) NOT NULL,
	token_digest CHAR(64) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	expires_at DATETIME(6) NOT NULL,
	consumed_at DATETIME(6) NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uk_email_verification_tokens_digest UNIQUE (token_digest),
	CONSTRAINT fk_email_verification_tokens_account
		FOREIGN KEY (account_id) REFERENCES accounts (id),
	KEY idx_email_verification_tokens_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
