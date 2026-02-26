UPDATE users
SET role_id = 7
WHERE role_id = 1;

UPDATE users
SET role_id = 4
WHERE role_id = 2;

DELETE
FROM roles
WHERE id IN (1, 2);