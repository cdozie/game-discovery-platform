CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_name_trgm ON games USING gin (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_provider_name_trgm ON games USING gin (provider_name gin_trgm_ops);
