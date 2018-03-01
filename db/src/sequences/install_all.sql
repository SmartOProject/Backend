set define off
set feedback off
set timing off

exec prv_inst_utils.create_object('create sequence PRV_API_EXEC_ID_SEQ minvalue 0 maxvalue 9999999999999999999999999999 start with 1 increment by 1 nocache');
exec prv_inst_utils.create_object('create sequence PRV_DEBUG_ID_SEQ minvalue 0 maxvalue 9999999999999999999999999999 start with 1 increment by 1 nocache');
exec prv_inst_utils.create_object('create sequence PRV_LOG_ID_SEQ minvalue 0 maxvalue 9999999999999999999999999999 start with 1 increment by 1 cache 10000');



set define on
set feedback on
set timing on
