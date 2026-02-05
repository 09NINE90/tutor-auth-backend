-- 1. Создаем таблицу roles в схеме user_service
CREATE TABLE IF NOT EXISTS user_service.roles
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    name
    VARCHAR
(
    50
) NOT NULL UNIQUE,
    description VARCHAR
(
    100
),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

-- 2. Вставляем роли с правильным синтаксисом
INSERT INTO user_service.roles (name, description)
SELECT DISTINCT role_name,
                CASE
                    WHEN role_name = 'USER' THEN 'Обычный пользователь'
                    WHEN role_name = 'TUTOR' THEN 'Преподаватель'
                    WHEN role_name = 'STUDENT' THEN 'Студент'
                    ELSE 'Пользователь'
                    END as description
FROM (SELECT DISTINCT UNNEST(roles) as role_name
      FROM user_service.users) as unique_roles
WHERE role_name IN ('TUTOR', 'STUDENT', 'USER') ON CONFLICT (name) DO NOTHING;

-- 3. Создаем таблицу связи user_roles
CREATE TABLE IF NOT EXISTS user_service.user_roles
(
    user_id
    UUID
    NOT
    NULL
    REFERENCES
    user_service
    .
    users
(
    id
) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES user_service.roles
(
    id
)
  ON DELETE CASCADE,
    PRIMARY KEY
(
    user_id,
    role_id
)
    );

-- 4. Переносим связи пользователей с ролями
INSERT INTO user_service.user_roles (user_id, role_id)
SELECT u.id as user_id,
       r.id as role_id
FROM user_service.users u
         CROSS JOIN LATERAL UNNEST(u.roles) as role_name
JOIN user_service.roles r
ON r.name = role_name
WHERE r.name IN ('TUTOR', 'STUDENT', 'USER');

-- 5. Для пользователей без ролей добавляем STUDENT
INSERT INTO user_service.user_roles (user_id, role_id)
SELECT u.id                                                       as user_id,
       (SELECT id FROM user_service.roles WHERE name = 'STUDENT') as role_id
FROM user_service.users u
WHERE NOT EXISTS (SELECT 1
                  FROM user_service.user_roles ur
                  WHERE ur.user_id = u.id)
  AND EXISTS (SELECT 1 FROM user_service.roles WHERE name = 'STUDENT');

-- 6. Проверяем миграцию
SELECT COUNT(DISTINCT user_id) as migrated_users,
       COUNT(*)                as total_assignments,
       r.name                  as role_name,
       COUNT(*)                as role_count
FROM user_service.user_roles ur
         JOIN user_service.roles r ON ur.role_id = r.id
GROUP BY r.name
ORDER BY role_count DESC;

-- 7. Удаляем старый столбец roles из users (после проверки)
ALTER TABLE user_service.users DROP COLUMN IF EXISTS roles;