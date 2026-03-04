ALTER TABLE users
    ADD COLUMN username VARCHAR(50);

UPDATE users
SET username = email
WHERE username IS NULL;

ALTER TABLE users
    ALTER COLUMN username SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL;
