CREATE TABLE IF NOT EXISTS game_source_entries (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    catalog_source VARCHAR(64) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    created TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_game_source_entries_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_game_source_entries_source_external
    ON game_source_entries (catalog_source, external_id);

CREATE INDEX IF NOT EXISTS game_source_entries_game_id_idx
    ON game_source_entries (game_id);

INSERT INTO game_source_entries (game_id, catalog_source, external_id, created, last_modified)
SELECT g.id,
       CASE
           WHEN g.psn_id LIKE 'IGDB:%' THEN 'IGDB_PC_XBOX'
           ELSE 'PLAYSTATION_STORE'
       END,
       g.psn_id,
       COALESCE(g.created, NOW()),
       COALESCE(g.last_modified, NOW())
FROM games g
WHERE g.psn_id IS NOT NULL
  AND g.psn_id <> ''
ON CONFLICT (catalog_source, external_id) DO NOTHING;
