insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r,
     permissions p
where r.name in (
                 'TUTOR_FULL',
                 'TUTOR_LIMITED',
                 'TUTOR_VIEWER',
                 'MODERATOR',
                 'MANAGER',
                 'STUDENT_FULL',
                 'STUDENT_AUDIT'
    )
  and p.name in (
    'USER_UPDATE_OWN'
    );