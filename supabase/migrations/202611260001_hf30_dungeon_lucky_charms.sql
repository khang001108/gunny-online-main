-- HF30: replace the rare tribal wing reward with 5-20 lucky charms.
-- Retain wire type 2 for old clients (amount=1 is a historical wing).
-- Keep existing reward receipts, ownership, equipment, deck odds and all other RPC logic.
BEGIN;
DO $hf30$
DECLARE
  src text := pg_get_functiondef('public.gunny_tribal(text,jsonb)'::regprocedure);
  old_amount text := 'kind:=m.deck[slot+1];amount:=1;';
  new_amount text := 'kind:=m.deck[slot+1];amount:=case when kind=2 then 5+floor(random()*16)::integer else 1 end;';
  old_award text := 'lucky_charms=least(999999,lucky_charms+1),wings=wings|case when kind=2 then 1 else 0 end,' || chr(10) || '  wing_equipped=wing_equipped or kind=2,owned=';
  new_award text := 'lucky_charms=least(999999,lucky_charms+case when kind=2 then amount else 1 end),owned=';
BEGIN
  IF strpos(src,new_amount)>0 AND strpos(src,new_award)>0 AND strpos(src,old_award)=0 THEN
    RAISE NOTICE 'HF30 already installed';
    RETURN;
  END IF;
  IF (length(src)-length(replace(src,old_amount,'')))/length(old_amount) <> 1 OR
     (length(src)-length(replace(src,old_award,'')))/length(old_award) <> 1 THEN
    RAISE EXCEPTION 'HF30: unexpected gunny_tribal definition; no changes applied';
  END IF;
  src := replace(replace(src,old_amount,new_amount),old_award,new_award);
  EXECUTE src;
END;
$hf30$;
NOTIFY pgrst,'reload schema';
COMMIT;
