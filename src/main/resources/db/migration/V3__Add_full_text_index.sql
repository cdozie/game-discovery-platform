CREATE INDEX IF NOT EXISTS game_text_idx ON games USING gin (to_tsvector('english', name || ' ' || games.provider_name));
