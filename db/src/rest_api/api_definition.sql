i»?prompt PL/SQL Developer Export Tables for user SMARTO@AMAZONE.ORCL
prompt Created by MainUser on 13 Март 2018 г.
set feedback off
set define off

prompt Disabling triggers for PRV_API_RESOURCE...
alter table PRV_API_RESOURCE disable all triggers;
prompt Disabling triggers for PRV_API_RESOURCE_PARAM...
alter table PRV_API_RESOURCE_PARAM disable all triggers;
prompt Loading PRV_API_RESOURCE...
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('GET:auth', 'Authorization', null, 'begin :i#auth_login := :user_phone; :token := null; end;', null, 0, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('POST:contact', 'Add contact to contact list', null, 'insert into contact ' || chr(10) || '(' || chr(10) || '  contact_id, ' || chr(10) || '  contact_name, ' || chr(10) || '  owner_user_id, ' || chr(10) || '  contact_user_id' || chr(10) || ')' || chr(10) || 'select' || chr(10) || '  contact_id_seq.nextval,' || chr(10) || '  nvl(:name, (select first_name || '' '' || last_name from app_users au where au.user_id = :contact_user_id)),' || chr(10) || '  :user_id,' || chr(10) || '  :contact_user_id' || chr(10) || 'from dual', null, 0, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('GET:contact', 'Get contact list', null, 'select user_id id,' || chr(10) || '  contact_name name,' || chr(10) || '  user_phone, ' || chr(10) || '  first_name,' || chr(10) || '  last_name,' || chr(10) || '  img_link' || chr(10) || 'from app_users au' || chr(10) || 'inner join contact c on (au.user_id = c.contact_user_id)' || chr(10) || 'where c.owner_user_id = :user_id', null, 1, 1, 1, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('PUT:contact/{id}', 'Update specific contact', null, 'update contact set' || chr(10) || '  contact_name = :name', 'contact_user_id = :id and owner_user_id = :user_id', 0, 1, 1, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('GET:search_contact', 'Search contacts', null, 'select user_id id,' || chr(10) || '  user_phone, ' || chr(10) || '  first_name,' || chr(10) || '  last_name,' || chr(10) || '  img_link' || chr(10) || 'from app_users au', '(' || chr(10) || '  (:phone is null or regexp_replace(au.user_phone, ''[^0-9]'') = regexp_replace(:phone, ''[^0-9]''))' || chr(10) || '  and (:name is null or lower(first_name) like lower(:name) || ''%'' or lower(last_name) like lower(:name) || ''%'')' || chr(10) || ')', 1, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('POST:task_group', 'Add new task group', null, 'insert into task_groups' || chr(10) || '(' || chr(10) || '  group_id,' || chr(10) || '  group_name,' || chr(10) || '  owner_user_id,' || chr(10) || '  order_num' || chr(10) || ')' || chr(10) || 'values' || chr(10) || '(' || chr(10) || '  task_group_id_seq.nextval,' || chr(10) || '  :group_name,' || chr(10) || '  :user_id,' || chr(10) || '  :order_num' || chr(10) || ')' || chr(10) || 'returning group_id into :id', null, 0, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('PUT:task_group/{id}', 'Update task group', null, 'update task_groups set group_name = :group_name, order_num = :order_num', 'owner_user_id = :user_id and group_id = :id', 0, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('DELETE:task_group/{id}', 'Delete task group', null, 'delete from task_groups', 'owner_user_id = :user_id and group_id = :id', 0, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('GET:task_group', 'Get all task groups', null, 'select ' || chr(10) || '  group_id id,' || chr(10) || '  group_name,' || chr(10) || '  order_num ' || chr(10) || 'from task_groups', 'owner_user_id = :user_id', 1, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('DELETE:contact/{id}', 'Delete contact from list', null, 'delete from contact', 'contact_user_id = :id and owner_user_id = :user_id', 0, 1, 1, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('DELETE:task/{id}', 'Delete task', null, 'delete from tasks', 'owner_user_id = :user_id and task_id = :id', 0, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('PUT:task/{id}', 'Update task', null, 'update tasks set ' || chr(10) || '  group_id = prv_utils.convert(group_id, :group_id), ' || chr(10) || '  task_type_id = prv_utils.convert(task_type_id, :task_type_id), ' || chr(10) || '  task_descr = prv_utils.convert(task_descr, :task_descr), ' || chr(10) || '  target_date = prv_utils.convert(target_date, :target_date), ' || chr(10) || '  order_num = prv_utils.convert(order_num, :order_num)', 'owner_user_id = :user_id and task_id = :id', 0, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('POST:task', 'Add new task', null, 'insert into tasks' || chr(10) || '(' || chr(10) || '  task_id,' || chr(10) || '  group_id,' || chr(10) || '  owner_user_id,' || chr(10) || '  task_type_id,' || chr(10) || '  task_descr,' || chr(10) || '  target_date,' || chr(10) || '  order_num' || chr(10) || ')' || chr(10) || 'values' || chr(10) || '(' || chr(10) || '  task_id_seq.nextval,' || chr(10) || '  :group_id,' || chr(10) || '  :user_id,' || chr(10) || '  :task_type_id,' || chr(10) || '  :task_descr,' || chr(10) || '  :target_date,' || chr(10) || '  :order_num' || chr(10) || ')' || chr(10) || 'returning task_id into :id', null, 0, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('GET:task', 'Get all user tasks', null, 'select ' || chr(10) || '  task_id id, ' || chr(10) || '  group_id, ' || chr(10) || '  task_type_id, ' || chr(10) || '  task_descr, ' || chr(10) || '  target_date, ' || chr(10) || '  order_num ' || chr(10) || 'from tasks', 'owner_user_id = :user_id', 1, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('POST:register_user', 'Register new user', null, 'insert into app_users ' || chr(10) || '(' || chr(10) || '  user_id, ' || chr(10) || '  user_phone, ' || chr(10) || '  first_name,' || chr(10) || '  last_name,' || chr(10) || '  pwd' || chr(10) || ')' || chr(10) || 'values ' || chr(10) || '(' || chr(10) || '  app_user_id_seq.nextval,' || chr(10) || '  :phone,' || chr(10) || '  :first_name,' || chr(10) || '  :last_name,' || chr(10) || '  :pwd' || chr(10) || ')' || chr(10) || 'returning user_id into :id', null, 0, 0, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('POST:sync_contacts', 'Add users to contact list using phone list', null, 'begin' || chr(10) || '  prv_var.m_rows_affected := app_utils.sync_contacts(:user_id, :phone_list);' || chr(10) || 'end;', null, 0, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('PUT:user_position', 'Update current user Geo position', null, 'begin' || chr(10) || '  update app_user_position set' || chr(10) || '   x = :x,' || chr(10) || '   y = :y' || chr(10) || '  where user_id = :user_id;' || chr(10) || '  ' || chr(10) || '  if sql%rowcount = 0 then' || chr(10) || '    insert into app_user_position (user_id, x, y)' || chr(10) || '    values (:user_id, :x, :y);' || chr(10) || '  end if;' || chr(10) || 'end;', null, 0, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('GET:contact_user_position', 'Get positions of users in contact list', null, 'select user_id, x, y' || chr(10) || 'from app_user_position p', 'p.user_id in (select contact_user_id from contact where owner_user_id = :user_id)', 1, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('PUT:user', 'Update user profile fields', null, 'update app_users set' || chr(10) || '  user_phone = prv_utils.convert(user_phone, :user_phone),' || chr(10) || '  first_name = prv_utils.convert(first_name, :first_name),' || chr(10) || '  last_name = prv_utils.convert(last_name, :last_name),' || chr(10) || '  img_link = prv_utils.convert(img_link, :img_link)  ', 'user_id = :user_id', 0, 1, 0, null);
insert into PRV_API_RESOURCE (request_pattern, description, required_roles, sql_template, rls_condition, return_cursor, auth_required, enable_vpd, module)
values ('GET:user/{id}', 'Get current user', null, 'select user_id,' || chr(10) || '       user_phone,' || chr(10) || '       first_name,' || chr(10) || '       last_name,' || chr(10) || '       img_link' || chr(10) || 'from app_users', 'user_id = :user_id', 1, 1, 0, null);
commit;
prompt 20 records loaded
prompt Loading PRV_API_RESOURCE_PARAM...
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:auth', 'i#auth_login', 'Login name', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:auth', 'token', 'JWT token', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('DELETE:contact/{id}', 'id', 'Template ID', 0, 1, 'integer', 'path', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('DELETE:contact/{id}', 'rows_affected', 'Number of rows affected by the method', 1, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact', 'first_rec', 'Pagination: first record', 0, 0, 'integer', 'query', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact', 'id', 'User ID', 1, 1, 'integer', 'body', 1, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact', 'orderby_clause', 'Sort expression: list of column numbers separated by comma', 0, 0, 'string', 'query', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact', 'rec_count', 'Pagination: maximum number of records', 0, 0, 'integer', 'query', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact/{id}', 'first_rec', 'Pagination: first record', 0, 0, 'integer', 'query', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact/{id}', 'id', 'Template ID', 0, 1, 'integer', 'path', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact/{id}', 'id', 'Template ID', 1, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact/{id}', 'name', 'Contact name', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact/{id}', 'rec_count', 'Pagination: maximum number of records', 0, 0, 'integer', 'query', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:contact/{id}', 'id', 'Contact ID', 0, 1, 'integer', 'path', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:contact/{id}', 'name', 'Contact name', 0, 0, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:contact/{id}', 'rows_affected', 'Number of rows affected by the method', 1, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:register_user', 'pwd', 'Password', 0, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:register_user', 'first_name', 'First name', 0, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:register_user', 'phone', 'Phone number', 0, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:contact', 'name', 'Contact name (if not set name will be equal to first name and last name)', 0, 0, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:contact', 'contact_user_id', 'Contact user id', 0, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:search_contact', 'last_name', 'Last name', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact', 'user_phone', 'User phone', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact', 'first_name', 'First name', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:search_contact', 'user_phone', 'User phone', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact', 'last_name', 'Last name', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:search_contact', 'first_name', 'First name', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:task_group', 'group_name', 'Group name', 0, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact', 'img_link', 'Profile image link', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:user', 'first_name', 'First name', 0, 0, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:search_contact', 'phone', 'Search by phone', 0, 0, 'string', 'query', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:search_contact', 'name', 'Search by name (last name or first name)', 0, 0, 'string', 'query', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:search_contact', 'img_link', 'Profile image link', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:task_group', 'order_num', 'Sort order number', 0, 0, 'number', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:task_group', 'id', 'Group ID', 1, 1, 'number', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:task_group', 'group_name', 'Group name', 1, 1, 'string', 'body', 2, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:task_group', 'order_num', 'Sort order number', 1, 1, 'number', 'body', 3, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:task_group', 'id', 'Group ID', 1, 1, 'number', 'body', 1, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('DELETE:task_group/{id}', 'id', 'Group ID', 0, 1, 'number', 'path', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('DELETE:task_group/{id}', 'rows_affected', 'Number of deleted rows', 1, 1, 'number', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:task_group/{id}', 'id', 'Group ID', 0, 1, 'number', 'path', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:task_group/{id}', 'group_name', 'Group name', 0, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:task_group/{id}', 'order_num', 'Sort order number', 0, 0, 'number', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:task_group/{id}', 'rows_affected', 'Number of deleted rows', 1, 1, 'number', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:user', 'img_link', 'Profile image link', 0, 0, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:user', 'last_name', 'Last name', 0, 0, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:user', 'user_phone', 'User phone', 0, 0, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:register_user', 'last_name', 'Last name', 0, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('DELETE:conact/{id}', 'id', 'User ID', 0, 1, 'integer', 'path', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:register_user', 'id', 'Created user ID', 1, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact', 'name', 'Contact name', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('DELETE:conact/{id}', 'rows_affected', 'Number of deleted records', 1, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:search_contact', 'id', 'User ID', 1, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('DELETE:task/{id}', 'rows_affected', 'Number of updated records', 1, 0, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('DELETE:task/{id}', 'id', 'Task ID', 0, 1, 'integer', 'path', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:task', 'id', 'Task ID', 1, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:task/{id}', 'rows_affected', 'Number of updated records', 1, 0, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:task', 'order_num', 'Order number', 1, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:task', 'target_date', 'Target task date', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:task', 'task_descr', 'Description', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:task', 'task_type_id', 'Task type ID', 1, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:task', 'group_id', 'Link to task group', 1, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:task/{id}', 'order_num', 'Order number', 0, 0, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:task/{id}', 'target_date', 'Target task date', 0, 0, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:task/{id}', 'task_descr', 'Description', 0, 0, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:task/{id}', 'task_type_id', 'Task type ID', 0, 0, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:task/{id}', 'group_id', 'Link to task group', 0, 0, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:task/{id}', 'id', 'Task ID', 0, 1, 'integer', 'path', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:task', 'order_num', 'Order number', 0, 0, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:task', 'target_date', 'Target task date', 0, 0, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:task', 'task_descr', 'Description', 0, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:task', 'task_type_id', 'Task type ID', 0, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:task', 'group_id', 'Link to task group', 0, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:task', 'id', 'Task ID', 1, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:user/{id}', 'first_name', 'First name', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:user/{id}', 'user_phone', 'User phone', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:user/{id}', 'last_name', 'Last name', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:user/{id}', 'img_link', 'Profile image link', 1, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:sync_contacts', 'phone_list', 'List of phones (Json Array)', 0, 1, 'string', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('POST:sync_contacts', 'rows_affected', 'Number of inserted rows', 1, 1, 'integer', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:user_position', 'x', 'X - coord', 0, 1, 'number', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('PUT:user_position', 'y', 'Y - coord', 0, 1, 'number', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact_user_position', 'x', 'X - coord', 1, 1, 'number', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact_user_position', 'y', 'Y - coord', 1, 1, 'number', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:contact_user_position', 'user_id', 'User ID', 1, 1, 'number', 'body', null, null);
insert into PRV_API_RESOURCE_PARAM (request_pattern, param_name, param_descr, param_direction, param_required, param_type, param_in, order_num, validation_json)
values ('GET:user/{id}', 'user_id', 'User ID', 1, 1, 'string', 'body', null, null);
commit;
prompt 86 records loaded
prompt Enabling triggers for PRV_API_RESOURCE...
alter table PRV_API_RESOURCE enable all triggers;
prompt Enabling triggers for PRV_API_RESOURCE_PARAM...
alter table PRV_API_RESOURCE_PARAM enable all triggers;

set feedback on
set define on
prompt Done
