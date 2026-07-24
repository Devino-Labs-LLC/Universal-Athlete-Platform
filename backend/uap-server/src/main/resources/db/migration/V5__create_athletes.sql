-- Athlete profiles foundation.
-- AccountId is a cross-context reference only; no FK to Identity accounts.

CREATE TABLE athletes (
	id BINARY(16) NOT NULL,
	account_id BINARY(16) NOT NULL,
	first_name VARCHAR(100) NOT NULL,
	last_name VARCHAR(100) NOT NULL,
	date_of_birth DATE NOT NULL,
	sex VARCHAR(20) NOT NULL,
	height_cm DECIMAL(5,2) NOT NULL,
	weight_kg DECIMAL(6,2) NOT NULL,
	dominant_hand VARCHAR(20) NOT NULL,
	dominant_foot VARCHAR(20) NOT NULL,
	status VARCHAR(20) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uk_athletes_account_id UNIQUE (account_id),
	INDEX idx_athletes_status (status),
	INDEX idx_athletes_last_name (last_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
