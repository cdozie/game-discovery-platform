CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS games (
    id BIGINT PRIMARY KEY,
    psn_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    categories VARCHAR(255) NOT NULL,
    platforms VARCHAR(255) NOT NULL,
    price VARCHAR(255) NOT NULL,
    url VARCHAR(255) NOT NULL,
    release_date VARCHAR(255) NOT NULL,
    provider_name VARCHAR(255) NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    created TIMESTAMPTZ NOT NULL,
    last_modified TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS games_name_index ON games (name);

CREATE TABLE IF NOT EXISTS game_stats (
    game_id BIGINT PRIMARY KEY,
    interactions BIGINT NOT NULL DEFAULT 0,
    wishlist_total BIGINT NOT NULL DEFAULT 0,
    comments BIGINT NOT NULL DEFAULT 0,
    posts BIGINT NOT NULL DEFAULT 0,
    average_rating DOUBLE PRECISION,
    CONSTRAINT fk_game_stats_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS wishlist (
    id BIGSERIAL PRIMARY KEY,
    userinfo BIGINT,
    gameinfo BIGINT,
    rating DOUBLE PRECISION,
    status VARCHAR(255) NOT NULL,
    CONSTRAINT uq_wishlist_user_game UNIQUE (userinfo, gameinfo),
    CONSTRAINT fk_wishlist_user FOREIGN KEY (userinfo) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_game FOREIGN KEY (gameinfo) REFERENCES games (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS wishlist_user_id_index ON wishlist (userinfo);

CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    author VARCHAR(255) NOT NULL,
    date VARCHAR(255) NOT NULL,
    post BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(255) NOT NULL,
    date VARCHAR(255) NOT NULL,
    game BIGINT NOT NULL
);
