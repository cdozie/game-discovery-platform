ALTER TABLE admin_audit_logs
    ALTER COLUMN details_json TYPE TEXT
    USING details_json::text;
