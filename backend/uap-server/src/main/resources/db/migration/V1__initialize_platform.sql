-- Universal Athlete Platform foundational bootstrap.
-- Domain tables (identity, athletes, etc.) are intentionally deferred.
-- Flyway owns schema evolution; Hibernate ddl-auto remains validate-only.

-- Establish utf8mb4 character-set and collation expectations for the current database.
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
