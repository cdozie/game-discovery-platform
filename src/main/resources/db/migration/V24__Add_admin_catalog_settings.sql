CREATE TABLE IF NOT EXISTS admin_catalog_settings (
    id BIGINT PRIMARY KEY,
    spotlight_game_id BIGINT REFERENCES games(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL
);
