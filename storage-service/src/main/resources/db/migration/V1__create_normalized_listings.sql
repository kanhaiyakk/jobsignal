CREATE TABLE normalized_listings (
    id              UUID            PRIMARY KEY,
    raw_listing_id  VARCHAR(255),
    source          VARCHAR(50)     NOT NULL,
    external_id     VARCHAR(255)    NOT NULL,
    title           VARCHAR(500)    NOT NULL,
    company         VARCHAR(255),
    location        VARCHAR(255),
    description_text TEXT,
    apply_url       VARCHAR(1000),
    posted_at       TIMESTAMPTZ,
    is_remote       BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_normalized_listings_source_external_id
    ON normalized_listings (source, external_id);

CREATE INDEX idx_normalized_listings_created_at
    ON normalized_listings (created_at DESC);

CREATE INDEX idx_normalized_listings_is_remote
    ON normalized_listings (is_remote);
