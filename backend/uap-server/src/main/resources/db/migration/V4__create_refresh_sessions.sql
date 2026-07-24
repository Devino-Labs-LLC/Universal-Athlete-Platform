-- Refresh sessions for credential authentication.
-- Persists refresh-token digests only; raw tokens are never stored.
-- Reuse of a rotated/revoked token invalidates the account session family.

CREATE TABLE refresh_sessions (
	id BINARY(16) NOT NULL,
	account_id BINARY(16) NOT NULL,
	token_digest CHAR(64) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	expires_at DATETIME(6) NOT NULL,
	last_used_at DATETIME(6) NULL,
	revoked_at DATETIME(6) NULL,
	replaced_by_session_id BINARY(16) NULL,
	revocation_reason VARCHAR(32) NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uk_refresh_sessions_token_digest UNIQUE (token_digest),
	CONSTRAINT fk_refresh_sessions_account
		FOREIGN KEY (account_id) REFERENCES accounts (id),
	CONSTRAINT fk_refresh_sessions_replaced_by
		FOREIGN KEY (replaced_by_session_id) REFERENCES refresh_sessions (id),
	KEY idx_refresh_sessions_account_active (account_id, revoked_at, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
