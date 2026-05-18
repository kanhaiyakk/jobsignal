CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE enriched_listings (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    normalized_listing_id VARCHAR(255),
    source               VARCHAR(100) NOT NULL,
    external_id          VARCHAR(255) NOT NULL,
    title                VARCHAR(500),
    company              VARCHAR(255),
    location             VARCHAR(255),
    description_text     TEXT,
    apply_url            VARCHAR(1000),
    posted_at            TIMESTAMPTZ,
    is_remote            BOOLEAN      NOT NULL DEFAULT FALSE,
    tech_stack           JSONB,
    seniority            VARCHAR(50),
    salary_range         VARCHAR(100),
    experience_years     INTEGER,
    remote_policy        VARCHAR(50),
    content_hash         VARCHAR(64)  NOT NULL,
    embedding            vector(768),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_enriched_source_external ON enriched_listings (source, external_id);
CREATE INDEX idx_enriched_created_at ON enriched_listings (created_at DESC);
CREATE INDEX idx_enriched_embedding ON enriched_listings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
