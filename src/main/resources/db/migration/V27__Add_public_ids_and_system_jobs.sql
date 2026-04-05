CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE users ADD COLUMN IF NOT EXISTS public_id VARCHAR(64);
UPDATE users SET public_id = gen_random_uuid()::text WHERE public_id IS NULL OR BTRIM(public_id) = '';
ALTER TABLE users ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS users_public_id_idx ON users(public_id);

ALTER TABLE games ADD COLUMN IF NOT EXISTS public_id VARCHAR(64);
UPDATE games SET public_id = gen_random_uuid()::text WHERE public_id IS NULL OR BTRIM(public_id) = '';
ALTER TABLE games ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS games_public_id_idx ON games(public_id);

ALTER TABLE game_comments ADD COLUMN IF NOT EXISTS public_id VARCHAR(64);
UPDATE game_comments SET public_id = gen_random_uuid()::text WHERE public_id IS NULL OR BTRIM(public_id) = '';
ALTER TABLE game_comments ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS game_comments_public_id_idx ON game_comments(public_id);

ALTER TABLE game_submissions ADD COLUMN IF NOT EXISTS public_id VARCHAR(64);
UPDATE game_submissions SET public_id = gen_random_uuid()::text WHERE public_id IS NULL OR BTRIM(public_id) = '';
ALTER TABLE game_submissions ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS game_submissions_public_id_idx ON game_submissions(public_id);

ALTER TABLE catalog_sync_runs ADD COLUMN IF NOT EXISTS public_id VARCHAR(64);
UPDATE catalog_sync_runs SET public_id = gen_random_uuid()::text WHERE public_id IS NULL OR BTRIM(public_id) = '';
ALTER TABLE catalog_sync_runs ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS catalog_sync_runs_public_id_idx ON catalog_sync_runs(public_id);

ALTER TABLE admin_audit_logs ADD COLUMN IF NOT EXISTS public_id VARCHAR(64);
UPDATE admin_audit_logs SET public_id = gen_random_uuid()::text WHERE public_id IS NULL OR BTRIM(public_id) = '';
ALTER TABLE admin_audit_logs ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS admin_audit_logs_public_id_idx ON admin_audit_logs(public_id);

CREATE TABLE IF NOT EXISTS system_jobs (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL,
    job_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_by VARCHAR(255) NOT NULL,
    requested_by_user_id BIGINT,
    catalog_source VARCHAR(64),
    parameters_json TEXT,
    progress_message TEXT,
    result_summary TEXT,
    error_message TEXT,
    related_sync_run_id BIGINT REFERENCES catalog_sync_runs(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS system_jobs_public_id_idx ON system_jobs(public_id);
CREATE INDEX IF NOT EXISTS system_jobs_created_at_idx ON system_jobs(created_at DESC);
CREATE INDEX IF NOT EXISTS system_jobs_status_idx ON system_jobs(status);
CREATE INDEX IF NOT EXISTS system_jobs_job_type_idx ON system_jobs(job_type);
