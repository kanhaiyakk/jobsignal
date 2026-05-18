CREATE TABLE listing_snapshots (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    enriched_event_id VARCHAR(255) NOT NULL,
    source            VARCHAR(100) NOT NULL,
    external_id       VARCHAR(255) NOT NULL,
    title             VARCHAR(500),
    company           VARCHAR(255),
    seniority         VARCHAR(50),
    remote_policy     VARCHAR(50),
    tech_stack        JSONB,
    salary_range      VARCHAR(100),
    experience_years  INTEGER,
    is_remote         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_snapshot_source_external ON listing_snapshots (source, external_id);
CREATE INDEX idx_snapshot_created_at ON listing_snapshots (created_at DESC);
CREATE INDEX idx_snapshot_seniority ON listing_snapshots (seniority);
CREATE INDEX idx_snapshot_remote_policy ON listing_snapshots (remote_policy);

CREATE TABLE weekly_reports (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    week_start           DATE        NOT NULL,
    week_end             DATE        NOT NULL,
    total_listings       INTEGER     NOT NULL,
    top_skills           JSONB,
    seniority_counts     JSONB,
    remote_policy_counts JSONB,
    top_companies        JSONB,
    report_text          TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_report_week ON weekly_reports (week_start, week_end);
CREATE INDEX idx_report_week_end ON weekly_reports (week_end DESC);
