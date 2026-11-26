-- Preserve the live crafting function, including earlier spear fixes and odds.
BEGIN;
DO $hf31$
DECLARE
  f regprocedure := to_regprocedure('public.gunny_crafting(uuid,text,jsonb,text,integer)');
  body text;
  old text := 'if owned<qty or lucky and p.lucky_charms<1 or protect and p.protection_charms<1 then raise exception ''Insufficient stones or charms'';end if;';
  replacement text := $new$if lucky and p.lucky_charms<1 and protect and p.protection_charms<1 then raise exception 'Insufficient lucky and protection charms';end if;
  if lucky and p.lucky_charms<1 then raise exception 'Insufficient lucky charms';end if;
  if protect and p.protection_charms<1 then raise exception 'Insufficient protection charms';end if;
  if owned<qty then raise exception 'Insufficient stones';end if;$new$;
BEGIN
  IF f IS NULL THEN RAISE EXCEPTION 'HF31: gunny_crafting function not found'; END IF;
  body := pg_get_functiondef(f);
  IF strpos(body,replacement)>0 THEN RAISE NOTICE 'HF31 already installed'; RETURN; END IF;
  IF length(body)-length(replace(body,old,''))<>length(old) THEN
    RAISE EXCEPTION 'HF31: crafting source differs; no change applied';
  END IF;
  EXECUTE replace(body,old,replacement);
END;
$hf31$;
COMMIT;
