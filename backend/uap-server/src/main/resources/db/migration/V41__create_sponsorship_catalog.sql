CREATE TABLE sponsors (
    id BINARY(16) NOT NULL,
    name VARCHAR(120) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL,
    logo_url VARCHAR(500) NULL,
    website_url VARCHAR(500) NULL,
    description VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_sponsors_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED'))
);

CREATE TABLE sponsorship_campaigns (
    id BINARY(16) NOT NULL,
    sponsor_id BINARY(16) NOT NULL,
    name VARCHAR(160) NOT NULL,
    status VARCHAR(16) NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NULL,
    headline VARCHAR(160) NOT NULL,
    body VARCHAR(500) NOT NULL,
    cta_label VARCHAR(40) NOT NULL,
    destination_url VARCHAR(500) NOT NULL,
    creative_url VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_campaigns_sponsor FOREIGN KEY (sponsor_id) REFERENCES sponsors (id),
    CONSTRAINT ck_campaigns_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'ACTIVE', 'PAUSED', 'ENDED', 'ARCHIVED')),
    INDEX idx_campaigns_sponsor (sponsor_id, status)
);

CREATE TABLE sponsorship_placements (
    id BINARY(16) NOT NULL,
    campaign_id BINARY(16) NOT NULL,
    surface VARCHAR(16) NOT NULL,
    audience VARCHAR(16) NOT NULL,
    organization_id BINARY(16) NULL,
    team_id BINARY(16) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_placements_campaign FOREIGN KEY (campaign_id) REFERENCES sponsorship_campaigns (id),
    CONSTRAINT ck_placements_surface CHECK (surface IN ('HOME', 'TRAINING', 'TEAM')),
    CONSTRAINT ck_placements_audience CHECK (audience IN ('ALL', 'ORGANIZATION', 'TEAM')),
    INDEX idx_placements_campaign (campaign_id),
    INDEX idx_placements_surface (surface)
);

CREATE TABLE sponsorship_events (
    id BINARY(16) NOT NULL,
    campaign_id BINARY(16) NOT NULL,
    placement_id BINARY(16) NOT NULL,
    surface VARCHAR(16) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    account_id BINARY(16) NOT NULL,
    bucket DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_events_campaign FOREIGN KEY (campaign_id) REFERENCES sponsorship_campaigns (id),
    CONSTRAINT ck_events_kind CHECK (kind IN ('IMPRESSION', 'CLICK')),
    UNIQUE KEY uk_sponsorship_event_dedupe (account_id, campaign_id, surface, kind, bucket),
    INDEX idx_events_campaign_bucket (campaign_id, surface, kind, bucket)
);
