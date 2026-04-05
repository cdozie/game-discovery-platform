ALTER TABLE games
    ADD COLUMN IF NOT EXISTS catalog_group_key VARCHAR(255);

CREATE INDEX IF NOT EXISTS games_catalog_group_key_idx
    ON games (catalog_group_key);

ALTER TABLE wishlist
    ADD COLUMN IF NOT EXISTS selected_platform VARCHAR(255);
