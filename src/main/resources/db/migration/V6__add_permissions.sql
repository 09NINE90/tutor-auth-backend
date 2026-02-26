create table permissions
(
    id          bigserial primary key,
    name        varchar(100) not null unique,
    description varchar(200),
    category    varchar(50),
    created_at  timestamp with time zone default now()
);

comment
on table permissions is 'Разрешения (что можно делать)';
comment
on column permissions.name is 'Уникальное имя разрешения';
comment
on column permissions.category is 'Категория для группировки';

-- Индексы для поиска
create index idx_permissions_name on permissions (name);
create index idx_permissions_category on permissions (category);

create table role_permissions
(
    role_id       bigint not null references roles (id) on delete cascade,
    permission_id bigint not null references permissions (id) on delete cascade,
    created_at    timestamp with time zone default now(),
    primary key (role_id, permission_id)
);

comment
on table role_permissions is 'Какие разрешения дают роли';

-- Индекс для быстрого поиска
create index idx_role_permissions_role_id on role_permissions (role_id);
create index idx_role_permissions_permission_id on role_permissions (permission_id);


-- Разрешения (permissions)
insert into permissions (name, description, category)
values
-- Курсы
('COURSE_CREATE', 'Создание курсов', 'COURSE'),
('COURSE_READ', 'Просмотр любых курсов', 'COURSE'),
('COURSE_READ_OWN', 'Просмотр своих курсов', 'COURSE'),
('COURSE_READ_ENROLLED', 'Просмотр курсов, на которые записан', 'COURSE'),
('COURSE_UPDATE', 'Редактирование любых курсов', 'COURSE'),
('COURSE_UPDATE_OWN', 'Редактирование своих курсов', 'COURSE'),
('COURSE_DELETE', 'Удаление любых курсов', 'COURSE'),
('COURSE_DELETE_OWN', 'Удаление своих курсов', 'COURSE'),
('COURSE_MEMBERS_VIEW', 'Просмотр участников курса', 'COURSE'),
('COURSE_INVITE_SEND', 'Отправка приглашений в курс', 'COURSE'),
('COURSE_INVITE_CONFIRM', 'Подтверждение приглашений', 'COURSE'),
('COURSE_ARCHIVE', 'Архивация курсов', 'COURSE'),

-- Задания
('TASK_CREATE', 'Создание заданий', 'TASK'),
('TASK_READ', 'Просмотр любых заданий', 'TASK'),
('TASK_READ_OWN', 'Просмотр своих заданий', 'TASK'),
('TASK_UPDATE', 'Редактирование любых заданий', 'TASK'),
('TASK_UPDATE_OWN', 'Редактирование своих заданий', 'TASK'),
('TASK_DELETE', 'Удаление любых заданий', 'TASK'),
('TASK_DELETE_OWN', 'Удаление своих заданий', 'TASK'),
('TASK_GRADE', 'Оценивание заданий', 'TASK'),
('TASK_FILE_UPLOAD', 'Загрузка файлов к заданию', 'TASK'),
('TASK_SUBMIT', 'Отправка ответов на задания', 'TASK'),

-- Пользователи
('USER_READ', 'Просмотр профилей пользователей', 'USER'),
('USER_UPDATE', 'Редактирование любых профилей', 'USER'),
('USER_UPDATE_OWN', 'Редактирование своего профиля', 'USER'),
('USER_DELETE', 'Удаление пользователей', 'USER'),
('USER_BLOCK', 'Блокировка пользователей', 'USER'),
('USER_ROLE_MANAGE', 'Управление ролями пользователей', 'USER'),

-- Системные
('DASHBOARD_VIEW', 'Просмотр дашборда', 'SYSTEM'),
('ADMIN_PANEL_ACCESS', 'Доступ в админ-панель', 'SYSTEM');

insert into roles (name, description)
values ('ADMIN', 'Полный доступ ко всем функциям'),
       ('TUTOR_FULL', 'Преподаватель с полными правами'),
       ('TUTOR_LIMITED', 'Преподаватель без права удаления'),
       ('TUTOR_VIEWER', 'Ассистент (только просмотр)'),
       ('STUDENT_FULL', 'Студент с полными правами'),
       ('STUDENT_AUDIT', 'Слушатель (без сдачи заданий)'),
       ('MODERATOR', 'Модератор контента'),
       ('MANAGER', 'Менеджер');

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'ADMIN';

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'TUTOR_FULL'
  and p.name in (
                 'COURSE_CREATE', 'COURSE_READ_OWN', 'COURSE_READ_ENROLLED',
                 'COURSE_UPDATE_OWN', 'COURSE_DELETE_OWN', 'COURSE_MEMBERS_VIEW',
                 'COURSE_INVITE_SEND', 'COURSE_INVITE_CONFIRM',
                 'TASK_CREATE', 'TASK_READ_OWN', 'TASK_UPDATE_OWN',
                 'TASK_DELETE_OWN', 'TASK_GRADE', 'TASK_FILE_UPLOAD',
                 'USER_READ', 'DASHBOARD_VIEW'
    );

-- TUTOR_LIMITED (без удаления)
insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'TUTOR_LIMITED'
  and p.name in (
                 'COURSE_CREATE', 'COURSE_READ_OWN', 'COURSE_READ_ENROLLED',
                 'COURSE_UPDATE_OWN', 'COURSE_MEMBERS_VIEW',
                 'COURSE_INVITE_SEND', 'COURSE_INVITE_CONFIRM',
                 'TASK_CREATE', 'TASK_READ_OWN', 'TASK_UPDATE_OWN',
                 'TASK_GRADE', 'TASK_FILE_UPLOAD',
                 'USER_READ', 'DASHBOARD_VIEW'
    );

-- TUTOR_VIEWER (только просмотр)
insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'TUTOR_VIEWER'
  and p.name in (
                 'COURSE_READ_OWN', 'COURSE_READ_ENROLLED',
                 'COURSE_MEMBERS_VIEW', 'TASK_READ_OWN',
                 'USER_READ', 'DASHBOARD_VIEW'
    );

-- STUDENT_FULL
insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'STUDENT_FULL'
  and p.name in (
                 'COURSE_READ_ENROLLED', 'TASK_READ', 'TASK_SUBMIT',
                 'USER_READ', 'DASHBOARD_VIEW'
    );

-- STUDENT_AUDIT (только просмотр)
insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'STUDENT_AUDIT'
  and p.name in (
                 'COURSE_READ_ENROLLED', 'TASK_READ',
                 'USER_READ', 'DASHBOARD_VIEW'
    );


-- MODERATOR
insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'MODERATOR'
  and p.name in (
                 'COURSE_READ', 'TASK_READ', 'USER_READ',
                 'COURSE_MEMBERS_VIEW', 'DASHBOARD_VIEW',
                 'USER_BLOCK'
    );

-- MANAGER
insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'MANAGER'
  and p.name in (
                 'COURSE_READ', 'COURSE_MEMBERS_VIEW',
                 'TASK_READ', 'USER_READ',
                 'USER_ROLE_MANAGE', 'DASHBOARD_VIEW'
    );


ALTER TABLE users ADD COLUMN role_id BIGINT REFERENCES roles(id);

UPDATE users u
SET role_id = ur.role_id
    FROM user_roles ur
WHERE u.id = ur.user_id;

ALTER TABLE users ALTER COLUMN role_id SET NOT NULL;

CREATE INDEX idx_users_role_id ON users(role_id);

DROP TABLE user_roles;