CREATE TABLE users_profiles
(
    id          UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL UNIQUE,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    birth_date  DATE,
    gender      VARCHAR(20)  NOT NULL    DEFAULT 'UNKNOWN',
    avatar_url  TEXT,

    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT chk_users_profiles_gender
        CHECK (gender IN ('MALE', 'FEMALE', 'UNKNOWN'))
);

CREATE INDEX idx_users_profiles_user_id ON users_profiles (user_id);
CREATE INDEX idx_users_profiles_first_name ON users_profiles (first_name);
CREATE INDEX idx_users_profiles_last_name ON users_profiles (last_name);

CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_profiles_updated_at
    BEFORE UPDATE
    ON users_profiles
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE users_profiles
    ADD CONSTRAINT fk_users_profiles_user_id
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;