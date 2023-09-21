CREATE INDEX game_text_idx ON games USING gin (
                                               (setweight(to_tsvector('english', name), 'A') ||
                                                setweight(to_tsvector('english', provider_name), 'B'))
    );
