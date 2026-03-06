insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name = 'STUDENT_FULL'
  and p.name in (
    'COURSE_INVITE_CONFIRM'
    );