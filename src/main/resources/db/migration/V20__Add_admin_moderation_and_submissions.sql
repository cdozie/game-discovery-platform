ALTER TABLE users
    ADD COLUMN IF NOT EXISTS ban_reason TEXT,
    ADD COLUMN IF NOT EXISTS ban_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS ban_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS ban_permanent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS banned_by_user_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_users_banned_by_user'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_banned_by_user
                FOREIGN KEY (banned_by_user_id) REFERENCES users (id) ON DELETE SET NULL;
    END IF;
END $$;

ALTER TABLE games
    ADD COLUMN IF NOT EXISTS admin_locked_fields TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS admin_last_modified_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS admin_last_modified_at TIMESTAMPTZ;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_games_admin_last_modified_by_user'
    ) THEN
        ALTER TABLE games
            ADD CONSTRAINT fk_games_admin_last_modified_by_user
                FOREIGN KEY (admin_last_modified_by_user_id) REFERENCES users (id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS game_submissions (
    id BIGSERIAL PRIMARY KEY,
    requested_by_user_id BIGINT NOT NULL,
    reviewed_by_user_id BIGINT,
    created_game_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    name VARCHAR(255) NOT NULL,
    description TEXT,
    categories TEXT,
    platforms TEXT,
    provider_name VARCHAR(255),
    image_url TEXT,
    release_date VARCHAR(255),
    url TEXT,
    review_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_game_submissions_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_game_submissions_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_game_submissions_created_game FOREIGN KEY (created_game_id) REFERENCES games (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS game_submissions_status_idx
    ON game_submissions (status, created_at DESC);

CREATE INDEX IF NOT EXISTS game_submissions_requested_by_idx
    ON game_submissions (requested_by_user_id, created_at DESC);
