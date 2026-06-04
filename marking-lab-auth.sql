DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id                  SERIAL          PRIMARY KEY,
    role                TEXT            NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'moderator', 'admin')),
    username            VARCHAR(255)    NOT NULL UNIQUE,
    email               VARCHAR(255)    NOT NULL UNIQUE,
    hashed_password     VARCHAR(255)    NOT NULL,
    token_hugging_face  VARCHAR(255),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);