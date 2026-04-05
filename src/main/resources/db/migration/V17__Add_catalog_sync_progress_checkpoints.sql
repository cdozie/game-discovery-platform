ALTER TABLE catalog_sync_runs
    ADD COLUMN IF NOT EXISTS requested_start_offset INTEGER NOT NULL DEFAULT 0;

ALTER TABLE catalog_sync_runs
    ADD COLUMN IF NOT EXISTS current_offset INTEGER NOT NULL DEFAULT 0;

ALTER TABLE catalog_sync_runs
    ADD COLUMN IF NOT EXISTS completed_batches INTEGER NOT NULL DEFAULT 0;

ALTER TABLE catalog_sync_runs
    ADD COLUMN IF NOT EXISTS last_checkpoint_at TIMESTAMPTZ;

UPDATE catalog_sync_runs
SET requested_start_offset = 0
WHERE requested_start_offset IS NULL;

UPDATE catalog_sync_runs
SET current_offset = 0
WHERE current_offset IS NULL;

UPDATE catalog_sync_runs
SET completed_batches = 0
WHERE completed_batches IS NULL;

CREATE INDEX IF NOT EXISTS catalog_sync_runs_catalog_source_status_started_at_idx
    ON catalog_sync_runs (catalog_source, status, started_at DESC);

