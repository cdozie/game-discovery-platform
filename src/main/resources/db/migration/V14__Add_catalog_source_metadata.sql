ALTER TABLE games
    ADD COLUMN IF NOT EXISTS catalog_sources VARCHAR(255);

UPDATE games
SET catalog_sources = 'PLAYSTATION_STORE'
WHERE catalog_sources IS NULL OR catalog_sources = '';

ALTER TABLE games
    ALTER COLUMN catalog_sources SET NOT NULL;

ALTER TABLE catalog_sync_runs
    ADD COLUMN IF NOT EXISTS catalog_source VARCHAR(64);

UPDATE catalog_sync_runs
SET catalog_source = 'PLAYSTATION_STORE'
WHERE catalog_source IS NULL OR catalog_source = '';

ALTER TABLE catalog_sync_runs
    ALTER COLUMN catalog_source SET NOT NULL;

CREATE INDEX IF NOT EXISTS catalog_sync_runs_catalog_source_idx ON catalog_sync_runs (catalog_source, started_at DESC);
