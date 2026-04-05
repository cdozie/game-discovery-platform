ALTER TABLE game_comments
    ADD COLUMN IF NOT EXISTS parent_comment_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_game_comments_parent'
    ) THEN
        ALTER TABLE game_comments
            ADD CONSTRAINT fk_game_comments_parent
                FOREIGN KEY (parent_comment_id) REFERENCES game_comments (id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS game_comments_parent_created_idx
    ON game_comments (parent_comment_id, created_at ASC);
