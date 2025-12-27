CREATE TABLE IF NOT EXISTS users
(
    id         UUID PRIMARY KEY,
    email      VARCHAR(255) UNIQUE NOT NULL,
    password   VARCHAR(255)        NOT NULL,
    roles      VARCHAR(50)[]       NOT NULL DEFAULT '{USER}',
    enabled    BOOLEAN                      DEFAULT true,
    created_at TIMESTAMP                    DEFAULT NOW()
);
