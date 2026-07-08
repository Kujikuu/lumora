-- Housekeeping: purge stale activation sessions (Phase 2E)

create or replace function public.purge_stale_activation_sessions(retention_days integer default 7)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  deleted_count integer;
begin
  delete from public.device_activation_sessions
  where created_at < now() - make_interval(days => retention_days);

  get diagnostics deleted_count = row_count;
  return deleted_count;
end;
$$;

grant execute on function public.purge_stale_activation_sessions(integer) to service_role;
