ALTER TABLE users
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

ALTER TABLE users
    ADD COLUMN created_at_tz TIMESTAMP WITH TIME ZONE;

UPDATE users
SET created_at_tz = created_at AT TIME ZONE 'UTC';


ALTER TABLE users
    DROP COLUMN created_at;

ALTER TABLE users
    RENAME COLUMN created_at_tz TO created_at;

CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE
    ON users
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON users (enabled);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users (created_at);