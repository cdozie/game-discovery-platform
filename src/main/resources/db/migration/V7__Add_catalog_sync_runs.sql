CREATE TABLE IF NOT EXISTS catalog_sync_runs (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    triggered_by VARCHAR(64) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    requested_page_size INTEGER,
    requested_max_batches INTEGER,
    requested_max_records INTEGER,
    processed_count BIGINT NOT NULL DEFAULT 0,
    created_count BIGINT NOT NULL DEFAULT 0,
    updated_count BIGINT NOT NULL DEFAULT 0,
    unchanged_count BIGINT NOT NULL DEFAULT 0,
    failed_count BIGINT NOT NULL DEFAULT 0,
    message TEXT
);

CREATE INDEX IF NOT EXISTS catalog_sync_runs_started_at_idx ON catalog_sync_runs (started_at DESC);
CREATE INDEX IF NOT EXISTS catalog_sync_runs_status_idx ON catalog_sync_runs (status);
