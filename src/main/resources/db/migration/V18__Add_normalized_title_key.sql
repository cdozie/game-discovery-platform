ALTER TABLE games
    ADD COLUMN IF NOT EXISTS normalized_title_key VARCHAR(255);

CREATE INDEX IF NOT EXISTS games_normalized_title_key_idx ON games (normalized_title_key);

