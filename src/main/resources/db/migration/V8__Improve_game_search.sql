CREATE INDEX IF NOT EXISTS game_search_document_idx ON games USING gin (
    (
        setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(provider_name, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(categories, '')), 'C') ||
        setweight(to_tsvector('english', coalesce(platforms, '')), 'C') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'D')
    )
);
