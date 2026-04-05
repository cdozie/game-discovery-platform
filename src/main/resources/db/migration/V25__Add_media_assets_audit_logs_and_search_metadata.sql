CREATE TABLE IF NOT EXISTS media_assets (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL,
    purpose VARCHAR(64) NOT NULL,
    content_type VARCHAR(160) NOT NULL,
    original_filename VARCHAR(255),
    storage_path TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_by_user_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT media_assets_public_id_uk UNIQUE (public_id),
    CONSTRAINT media_assets_storage_path_uk UNIQUE (storage_path),
    CONSTRAINT fk_media_assets_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS media_assets_created_by_idx
    ON media_assets (created_by_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS media_assets_purpose_idx
    ON media_assets (purpose, created_at DESC);

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT,
    actor_username VARCHAR(255),
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(128),
    summary TEXT NOT NULL,
    details_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_admin_audit_logs_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS admin_audit_logs_created_idx
    ON admin_audit_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS admin_audit_logs_actor_idx
    ON admin_audit_logs (actor_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS admin_audit_logs_entity_idx
    ON admin_audit_logs (entity_type, entity_id, created_at DESC);

CREATE TABLE IF NOT EXISTS game_platform_tags (
    game_id BIGINT NOT NULL,
    value VARCHAR(255) NOT NULL,
    normalized_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (game_id, normalized_value),
    CONSTRAINT fk_game_platform_tags_game
        FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS game_platform_tags_normalized_idx
    ON game_platform_tags (normalized_value, value);

CREATE TABLE IF NOT EXISTS game_category_tags (
    game_id BIGINT NOT NULL,
    value VARCHAR(255) NOT NULL,
    normalized_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (game_id, normalized_value),
    CONSTRAINT fk_game_category_tags_game
        FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS game_category_tags_normalized_idx
    ON game_category_tags (normalized_value, value);

CREATE TABLE IF NOT EXISTS game_catalog_source_tags (
    game_id BIGINT NOT NULL,
    value VARCHAR(255) NOT NULL,
    normalized_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (game_id, normalized_value),
    CONSTRAINT fk_game_catalog_source_tags_game
        FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS game_catalog_source_tags_normalized_idx
    ON game_catalog_source_tags (normalized_value, value);

CREATE TABLE IF NOT EXISTS game_publisher_tags (
    game_id BIGINT NOT NULL,
    value VARCHAR(255) NOT NULL,
    normalized_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (game_id, normalized_value),
    CONSTRAINT fk_game_publisher_tags_game
        FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS game_publisher_tags_normalized_idx
    ON game_publisher_tags (normalized_value, value);

CREATE OR REPLACE FUNCTION refresh_game_search_metadata() RETURNS TRIGGER AS $$
DECLARE
    target_game_id BIGINT;
BEGIN
    target_game_id := COALESCE(NEW.id, OLD.id);

    DELETE FROM game_platform_tags WHERE game_id = target_game_id;
    DELETE FROM game_category_tags WHERE game_id = target_game_id;
    DELETE FROM game_catalog_source_tags WHERE game_id = target_game_id;
    DELETE FROM game_publisher_tags WHERE game_id = target_game_id;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;

    INSERT INTO game_platform_tags (game_id, value, normalized_value)
    SELECT NEW.id,
           entry.value,
           LOWER(entry.value)
    FROM (
        SELECT DISTINCT BTRIM(token) AS value
        FROM regexp_split_to_table(COALESCE(NEW.platforms, ''), ',') AS token
        WHERE BTRIM(token) <> ''
    ) entry;

    INSERT INTO game_category_tags (game_id, value, normalized_value)
    SELECT NEW.id,
           entry.value,
           LOWER(entry.value)
    FROM (
        SELECT DISTINCT BTRIM(token) AS value
        FROM regexp_split_to_table(COALESCE(NEW.categories, ''), ',') AS token
        WHERE BTRIM(token) <> ''
    ) entry;

    INSERT INTO game_catalog_source_tags (game_id, value, normalized_value)
    SELECT NEW.id,
           entry.value,
           LOWER(entry.value)
    FROM (
        SELECT DISTINCT BTRIM(token) AS value
        FROM regexp_split_to_table(COALESCE(NEW.catalog_sources, ''), ',') AS token
        WHERE BTRIM(token) <> ''
    ) entry;

    IF NULLIF(BTRIM(COALESCE(NEW.provider_name, '')), '') IS NOT NULL THEN
        INSERT INTO game_publisher_tags (game_id, value, normalized_value)
        VALUES (NEW.id, BTRIM(NEW.provider_name), LOWER(BTRIM(NEW.provider_name)));
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_refresh_game_search_metadata ON games;

CREATE TRIGGER trg_refresh_game_search_metadata
AFTER INSERT OR UPDATE OF platforms, categories, catalog_sources, provider_name OR DELETE
ON games
FOR EACH ROW
EXECUTE FUNCTION refresh_game_search_metadata();

INSERT INTO game_platform_tags (game_id, value, normalized_value)
SELECT g.id,
       entry.value,
       LOWER(entry.value)
FROM games g
CROSS JOIN LATERAL (
    SELECT DISTINCT BTRIM(token) AS value
    FROM regexp_split_to_table(COALESCE(g.platforms, ''), ',') AS token
    WHERE BTRIM(token) <> ''
) entry
ON CONFLICT DO NOTHING;

INSERT INTO game_category_tags (game_id, value, normalized_value)
SELECT g.id,
       entry.value,
       LOWER(entry.value)
FROM games g
CROSS JOIN LATERAL (
    SELECT DISTINCT BTRIM(token) AS value
    FROM regexp_split_to_table(COALESCE(g.categories, ''), ',') AS token
    WHERE BTRIM(token) <> ''
) entry
ON CONFLICT DO NOTHING;

INSERT INTO game_catalog_source_tags (game_id, value, normalized_value)
SELECT g.id,
       entry.value,
       LOWER(entry.value)
FROM games g
CROSS JOIN LATERAL (
    SELECT DISTINCT BTRIM(token) AS value
    FROM regexp_split_to_table(COALESCE(g.catalog_sources, ''), ',') AS token
    WHERE BTRIM(token) <> ''
) entry
ON CONFLICT DO NOTHING;

INSERT INTO game_publisher_tags (game_id, value, normalized_value)
SELECT g.id,
       BTRIM(g.provider_name) AS value,
       LOWER(BTRIM(g.provider_name)) AS normalized_value
FROM games g
WHERE NULLIF(BTRIM(COALESCE(g.provider_name, '')), '') IS NOT NULL
ON CONFLICT DO NOTHING;
