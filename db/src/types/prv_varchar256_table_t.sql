begin
  prv_inst_utils.update_type('
create or replace type prv_varchar256_table_t as table of varchar2(256)');
end;
/