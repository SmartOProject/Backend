begin
  prv_inst_utils.update_type('
create or replace type prv_abstract_table_t as table of prv_abstract_rec_t');
end;
/