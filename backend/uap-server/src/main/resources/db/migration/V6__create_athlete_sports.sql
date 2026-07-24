-- Athlete sport participation.
-- Primary-sport uniqueness uses generated primary_slot (NULL when not primary; MySQL UNIQUE allows multiple NULLs).
-- Sport duplicate prevention uses generated sport_identity (sport_type, or OTHER:<normalized name>).

CREATE TABLE athlete_sports (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	sport_type VARCHAR(40) NOT NULL,
	custom_sport_name VARCHAR(100) NULL,
	custom_sport_name_normalized VARCHAR(100) NULL,
	is_primary BOOLEAN NOT NULL DEFAULT FALSE,
	participation_level VARCHAR(40) NOT NULL,
	preferred_position VARCHAR(100) NULL,
	years_experience SMALLINT UNSIGNED NOT NULL,
	season_status VARCHAR(30) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	sport_identity VARCHAR(145) GENERATED ALWAYS AS (
		CASE
			WHEN sport_type = 'OTHER' THEN CONCAT('OTHER:', IFNULL(custom_sport_name_normalized, ''))
			ELSE sport_type
		END
	) STORED,
	primary_slot BINARY(16) GENERATED ALWAYS AS (
		CASE WHEN is_primary THEN athlete_id ELSE NULL END
	) STORED,
	PRIMARY KEY (id),
	CONSTRAINT fk_athlete_sports_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT uk_athlete_sports_identity UNIQUE (athlete_id, sport_identity),
	CONSTRAINT uk_athlete_sports_primary UNIQUE (primary_slot),
	INDEX idx_athlete_sports_athlete_id (athlete_id),
	INDEX idx_athlete_sports_primary (athlete_id, is_primary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
