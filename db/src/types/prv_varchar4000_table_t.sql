begin
  prv_inst_utils.update_type('
create or replace type prv_varchar4000_table_t as table of varchar2(4000)');
end;
/