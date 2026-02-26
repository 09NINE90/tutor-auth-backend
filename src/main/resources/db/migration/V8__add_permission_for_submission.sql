insert into permissions (name, description, category)
values ('TASK_SUBMISSION_VIEW_ALL', 'Просмотр всех сданных работ по заданию', 'TASK'),
       ('TASK_SUBMISSION_VIEW_OWN', 'Просмотр своих сданных работ', 'TASK');

-- TUTOR_FULL получает permission на просмотр всех сдач
insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'TUTOR_FULL'
  and p.name = 'TASK_SUBMISSION_VIEW_ALL';

-- TUTOR_LIMITED тоже получает
insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'TUTOR_LIMITED'
  and p.name = 'TASK_SUBMISSION_VIEW_ALL';

-- TUTOR_VIEWER тоже может видеть все сдачи (только просмотр)
insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'TUTOR_VIEWER'
  and p.name = 'TASK_SUBMISSION_VIEW_ALL';

-- STUDENT_FULL получает permission на просмотр своих сдач
insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'STUDENT_FULL'
  and p.name = 'TASK_SUBMISSION_VIEW_OWN';

-- STUDENT_AUDIT тоже (хотя они не сдают, но могут посмотреть свои попытки)
insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'STUDENT_AUDIT'
  and p.name = 'TASK_SUBMISSION_VIEW_OWN';
