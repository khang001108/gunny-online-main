-- Read-only, one result row. Does not equip items or modify player data.
select
  current_database() as database_name,
  has_schema_privilege('service_role','public','USAGE') as service_schema_usage,
  to_regprocedure('public.gunny_equip(uuid,integer,integer)') as function_signature,
  pg_get_function_arguments(to_regprocedure('public.gunny_equip(uuid,integer,integer)')) as arguments,
  coalesce(has_function_privilege('service_role',to_regprocedure('public.gunny_equip(uuid,integer,integer)'),'EXECUTE'),false) as service_can_execute,
  coalesce((select prosecdef from pg_proc where oid=to_regprocedure('public.gunny_equip(uuid,integer,integer)')),false) as security_definer,
  array(
    select required.name
    from unnest(array['id','gender','owned','wardrobe','look','worn_mask','equipped','skin','custom_look']) as required(name)
    where not exists (
      select 1 from information_schema.columns c
      where c.table_schema='public' and c.table_name='profiles' and c.column_name=required.name
    )
  ) as missing_profile_columns;
