ALTER TABLE users
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS bio TEXT,
    ADD COLUMN IF NOT EXISTS avatar_url TEXT,
    ADD COLUMN IF NOT EXISTS favorite_platform VARCHAR(80),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_modified TIMESTAMPTZ;

UPDATE users
SET display_name = username
WHERE display_name IS NULL;

UPDATE users
SET created_at = NOW()
WHERE created_at IS NULL;

UPDATE users
SET last_modified = created_at
WHERE last_modified IS NULL;

ALTER TABLE users
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN last_modified SET DEFAULT NOW(),
    ALTER COLUMN last_modified SET NOT NULL;
