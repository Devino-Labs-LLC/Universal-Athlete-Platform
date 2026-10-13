CREATE TABLE marketplace_categories (
    code VARCHAR(32) NOT NULL,
    name VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (code)
);

INSERT INTO marketplace_categories (code, name, sort_order) VALUES
    ('TRAINING', 'Training', 1),
    ('RECOVERY', 'Recovery', 2),
    ('EQUIPMENT', 'Equipment', 3),
    ('APPAREL', 'Apparel', 4),
    ('NUTRITION', 'Nutrition', 5),
    ('SERVICES', 'Services', 6),
    ('CAMPS_PROGRAMS', 'Camps & Programs', 7);

CREATE TABLE marketplace_listings (
    id BINARY(16) NOT NULL,
    sponsor_id BINARY(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    short_description VARCHAR(280) NOT NULL,
    full_description VARCHAR(2000) NOT NULL,
    listing_type VARCHAR(16) NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    image_url VARCHAR(500) NULL,
    destination_url VARCHAR(500) NOT NULL,
    price_display VARCHAR(40) NULL,
    discount_text VARCHAR(160) NULL,
    promo_code VARCHAR(40) NULL,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    audience VARCHAR(16) NOT NULL,
    organization_id BINARY(16) NULL,
    team_id BINARY(16) NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NULL,
    offer_starts_at DATETIME(6) NULL,
    offer_ends_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_marketplace_listings_sponsor FOREIGN KEY (sponsor_id) REFERENCES sponsors (id),
    CONSTRAINT fk_marketplace_listings_category FOREIGN KEY (category_code) REFERENCES marketplace_categories (code),
    CONSTRAINT ck_marketplace_listing_type CHECK (listing_type IN ('PRODUCT', 'SERVICE', 'PROGRAM', 'EVENT', 'OFFER')),
    CONSTRAINT ck_marketplace_listing_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'ACTIVE', 'PAUSED', 'ENDED', 'ARCHIVED')),
    CONSTRAINT ck_marketplace_listing_audience CHECK (audience IN ('ALL', 'ORGANIZATION', 'TEAM')),
    INDEX idx_marketplace_listings_browse (status, category_code, listing_type, featured, updated_at)
);

CREATE TABLE marketplace_saves (
    id BINARY(16) NOT NULL,
    account_id BINARY(16) NOT NULL,
    listing_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_marketplace_saves_listing FOREIGN KEY (listing_id) REFERENCES marketplace_listings (id),
    UNIQUE KEY uk_marketplace_save (account_id, listing_id)
);

CREATE TABLE marketplace_events (
    id BINARY(16) NOT NULL,
    listing_id BINARY(16) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    account_id BINARY(16) NOT NULL,
    bucket DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_marketplace_events_listing FOREIGN KEY (listing_id) REFERENCES marketplace_listings (id),
    CONSTRAINT ck_marketplace_event_kind CHECK (kind IN ('IMPRESSION', 'DETAIL', 'CLICK', 'REDEEM', 'SAVE')),
    UNIQUE KEY uk_marketplace_event_dedupe (account_id, listing_id, kind, bucket),
    INDEX idx_marketplace_events_listing (listing_id, kind, bucket)
);
