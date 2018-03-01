create user smarto identified by "&pwd";
grant connect to smarto;
grant resource to smarto;

grant select on sys.v_$database to smarto;
grant select on sys.v_$instance to smarto;
grant select on sys.v_$open_cursor to smarto;
grant select on sys.v_$parameter to smarto;
grant select on sys.v_$process to smarto;
grant select on sys.v_$session to smarto;
grant select on sys.v_$sesstat to smarto;
grant select on sys.v_$sqlarea to smarto;
grant select on sys.v_$sqltext to smarto;

grant execute on sys.dbms_crypto to smarto;
grant execute on sys.dbms_hprof to smarto;
grant execute on sys.dbms_monitor to smarto;
grant execute on sys.dbms_lock to smarto;
grant execute on sys.dbms_network_acl_admin to smarto;

grant manage scheduler to smarto;
grant create job to smarto;
grant debug connect session to smarto;
grant create any context to smarto;
grant create trigger to smarto;
grant create procedure to smarto;
grant create database link to smarto;
grant create sequence to smarto;
grant create view to smarto;
grant create table to smarto;
grant unlimited tablespace to smarto;
grant alter tablespace to smarto;
grant alter session to smarto;
grant create synonym to smarto;

grant execute on dbms_network_acl_admin to smarto;

