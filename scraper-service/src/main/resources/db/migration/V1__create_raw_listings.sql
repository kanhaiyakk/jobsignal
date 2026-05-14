CREATE TABLE raw_listings (
    id              UUID PRIMARY KEY,
    source          VARCHAR(50) NOT NULL,
    external_id     VARCHAR(255) NOT NULL,
    title           VARCHAR(500) NOT NULL,
    company         VARCHAR(255),
    location        VARCHAR(255),
    description     TEXT,
    apply_url       VARCHAR(1000),
    posted_at       TIMESTAMPTZ,
    raw_payload     JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_raw_listings_source_external UNIQUE (source, external_id)
);

CREATE INDEX idx_raw_listings_source ON raw_listings (source);
CREATE INDEX idx_raw_listings_posted_at ON raw_listings (posted_at DESC);
CREATE INDEX idx_raw_listings_created_at ON raw_listings (created_at DESC);
