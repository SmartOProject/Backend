begin
  prv_inst_utils.update_type('
create or replace type prv_abstract_rec_t as object
(
  n1 number,
  n2 number,
  n3 number,
  c1 varchar2(4000),
  c2 varchar2(4000),
  c3 varchar2(4000)
)');
end;
/