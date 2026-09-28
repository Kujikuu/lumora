-- Activation security hardening
--
-- 1. Rows are created only through create_device_activation_session (server-side expiry).
-- 2. The TV polls with its session id + qr_token and only sees status + expiry.
-- 3. The exchange edge function claims an approved session atomically with the qr_token,
--    so seeing the code on screen is no longer enough to take the session.
-- 4. Purge is service-role only and runs daily when pg_cron is available.

-- ---------------------------------------------------------------------------
-- No direct table access for clients
-- ---------------------------------------------------------------------------
drop policy if exists "Anyone can create activation session" on public.device_activation_sessions;

revoke select, insert, update, delete on public.device_activation_sessions from anon, authenticated;

-- ---------------------------------------------------------------------------
-- TV status polling: requires the qr_token, returns only status + expiry
-- ---------------------------------------------------------------------------
drop function if exists public.get_device_activation_session(uuid);

create or replace function public.get_device_activation_session(
  session_id uuid,
  session_qr_token text
)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
  session_row public.device_activation_sessions;
begin
  select * into session_row
  from public.device_activation_sessions
  where id = session_id
    and qr_token = session_qr_token;

  if not found then
    return json_build_object('status', 'EXPIRED', 'expires_at', now());
  end if;

  return json_build_object(
    'status', case
      when session_row.status = 'PENDING' and session_row.expires_at <= now() then 'EXPIRED'
      else session_row.status
    end,
    'expires_at', session_row.expires_at
  );
end;
$$;

revoke all on function public.get_device_activation_session(uuid, text) from public;
grant execute on function public.get_device_activation_session(uuid, text) to anon, authenticated;

-- ---------------------------------------------------------------------------
-- Companion page: show which TV a code belongs to before the user approves it
-- ---------------------------------------------------------------------------
create or replace function public.preview_device_activation(activation_code text)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
  session_row public.device_activation_sessions;
begin
  if auth.uid() is null then
    raise exception 'Not authenticated';
  end if;

  select * into session_row
  from public.device_activation_sessions
  where code = upper(trim(activation_code))
    and status = 'PENDING'
    and expires_at > now();

  if not found then
    return null;
  end if;

  return json_build_object(
    'device_name', session_row.device_name,
    'expires_at', session_row.expires_at
  );
end;
$$;

revoke all on function public.preview_device_activation(text) from public;
grant execute on function public.preview_device_activation(text) to authenticated;

-- ---------------------------------------------------------------------------
-- Approve: same checks as 005, but never hand the row (with qr_token) to the phone
-- ---------------------------------------------------------------------------
drop function if exists public.approve_device_activation(text);

create or replace function public.approve_device_activation(activation_code text)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
  session_row public.device_activation_sessions;
  approving_user_id uuid := auth.uid();
begin
  if approving_user_id is null then
    raise exception 'Not authenticated';
  end if;

  update public.device_activation_sessions
  set status = 'APPROVED',
      user_id = approving_user_id
  where code = upper(trim(activation_code))
    and status = 'PENDING'
    and expires_at > now()
  returning * into session_row;

  if not found then
    raise exception 'Invalid or expired activation code';
  end if;

  return json_build_object('device_name', session_row.device_name);
end;
$$;

revoke all on function public.approve_device_activation(text) from public;
grant execute on function public.approve_device_activation(text) to authenticated;

-- ---------------------------------------------------------------------------
-- Exchange: atomic one-time claim, service role only
-- ---------------------------------------------------------------------------
create or replace function public.claim_approved_activation(
  session_id uuid,
  session_qr_token text
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  claimed_user_id uuid;
begin
  update public.device_activation_sessions
  set status = 'EXPIRED'
  where id = session_id
    and qr_token = session_qr_token
    and status = 'APPROVED'
    and expires_at > now()
  returning user_id into claimed_user_id;

  return claimed_user_id;
end;
$$;

revoke all on function public.claim_approved_activation(uuid, text) from public, anon, authenticated;
grant execute on function public.claim_approved_activation(uuid, text) to service_role;

-- ---------------------------------------------------------------------------
-- Purge: service role only, bounded retention
-- ---------------------------------------------------------------------------
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
  where created_at < now() - make_interval(days => greatest(coalesce(retention_days, 7), 1))
    and (status <> 'PENDING' or expires_at < now());

  get diagnostics deleted_count = row_count;
  return deleted_count;
end;
$$;

revoke all on function public.purge_stale_activation_sessions(integer) from public, anon, authenticated;
grant execute on function public.purge_stale_activation_sessions(integer) to service_role;

-- Daily purge when pg_cron is enabled (Dashboard > Database > Extensions). Without it,
-- run `select public.purge_stale_activation_sessions();` from a scheduled job.
do $$
begin
  if exists (select 1 from pg_extension where extname = 'pg_cron') then
    perform cron.unschedule(jobid)
    from cron.job
    where jobname = 'purge-stale-activation-sessions';

    perform cron.schedule(
      'purge-stale-activation-sessions',
      '17 3 * * *',
      'select public.purge_stale_activation_sessions(7);'
    );
  end if;
end;
$$;
