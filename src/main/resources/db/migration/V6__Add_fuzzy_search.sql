CREATE INDEX idx_name_trgm ON games USING gin (name gin_trgm_ops);
CREATE INDEX idx_provider_name_trgm ON games USING gin (provider_name gin_trgm_ops);