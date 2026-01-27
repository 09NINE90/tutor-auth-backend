ALTER TABLE users_profiles
    RENAME COLUMN avatar_url TO avatar_s3_key;

ALTER TABLE users_profiles
ALTER
COLUMN avatar_s3_key TYPE VARCHAR(50);

COMMENT
ON COLUMN users_profiles.avatar_s3_key IS 'Ключ для поулучения изображения из хранилища';