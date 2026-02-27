CREATE TABLE auth_events
(
    id            UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id       UUID         REFERENCES users (id) ON DELETE SET NULL,
    event_type    VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    ip_address    VARCHAR(45),
    user_agent    TEXT,
    device_info   TEXT,
    location      VARCHAR(255),
    error_code    VARCHAR(50),
    error_message TEXT,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для быстрого поиска
CREATE INDEX idx_auth_events_user_id ON auth_events (user_id);
CREATE INDEX idx_auth_events_email ON auth_events (email);
CREATE INDEX idx_auth_events_created_at ON auth_events (created_at);
CREATE INDEX idx_auth_events_event_type ON auth_events (event_type);

CREATE TABLE refresh_tokens
(
    id          UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id     UUID                     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(255)             NOT NULL UNIQUE,
    device_info TEXT,
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    revoked_at  TIMESTAMP WITH TIME ZONE,            -- когда был отозван (logout)
    replaced_by UUID REFERENCES refresh_tokens (id), -- для цепочки обновлений

    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Индексы
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
