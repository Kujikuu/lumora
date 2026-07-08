-- Harden activation RLS, profile ownership, and query indexes (Phase 1)

-- ---------------------------------------------------------------------------
-- Composite indexes for favorites / watch history / activation lookup
-- ---------------------------------------------------------------------------
create index if not exists favorites_profile_created_at_idx
  on public.favorites (profile_id, created_at desc);

create index if not exists watch_history_profile_last_watched_idx
  on public.watch_history (profile_id, last_watched_at desc);

create index if not exists device_activation_sessions_approved_code_idx
  on public.device_activation_sessions (code)
  where status = 'APPROVED';

-- ---------------------------------------------------------------------------
-- Profile ownership enforcement on user-scoped rows
-- ---------------------------------------------------------------------------
create or replace function public.enforce_profile_ownership()
returns trigger
language plpgsql
as $$
begin
  if not exists (
    select 1
    from public.profiles
    where id = new.profile_id
      and user_id = new.user_id
  ) then
    raise exception 'Profile does not belong to user';
  end if;
  return new;
end;
$$;

drop trigger if exists favorites_enforce_profile_ownership on public.favorites;
create trigger favorites_enforce_profile_ownership
before insert or update on public.favorites
for each row execute function public.enforce_profile_ownership();

drop trigger if exists watch_history_enforce_profile_ownership on public.watch_history;
create trigger watch_history_enforce_profile_ownership
before insert or update on public.watch_history
for each row execute function public.enforce_profile_ownership();

drop trigger if exists parental_controls_enforce_profile_ownership on public.parental_controls;
create trigger parental_controls_enforce_profile_ownership
before insert or update on public.parental_controls
for each row execute function public.enforce_profile_ownership();

-- ---------------------------------------------------------------------------
-- Activation session access: scoped read RPC, no direct UPDATE
-- ---------------------------------------------------------------------------
drop policy if exists "Anyone can read activation session by code or token" on public.device_activation_sessions;
drop policy if exists "Authenticated users can approve activation session" on public.device_activation_sessions;

revoke update on public.device_activation_sessions from authenticated;

create or replace function public.get_device_activation_session(session_id uuid)
returns public.device_activation_sessions
language plpgsql
security definer
set search_path = public
as $$
declare
  session_row public.device_activation_sessions;
begin
  select * into session_row
  from public.device_activation_sessions
  where id = session_id;

  if not found then
    return null;
  end if;

  return session_row;
end;
$$;

grant execute on function public.get_device_activation_session(uuid) to anon, authenticated;

-- ---------------------------------------------------------------------------
-- Fix create_device_activation_session: server-controlled expiry
-- ---------------------------------------------------------------------------
create or replace function public.create_device_activation_session(
  activation_code text,
  activation_qr_token text,
  activation_device_name text default null
)
returns public.device_activation_sessions
language plpgsql
security definer
set search_path = public
as $$
declare
  session_row public.device_activation_sessions;
begin
  insert into public.device_activation_sessions (
    code,
    qr_token,
    status,
    device_name,
    expires_at
  )
  values (
    upper(trim(activation_code)),
    activation_qr_token,
    'PENDING',
    activation_device_name,
    now() + interval '15 minutes'
  )
  returning * into session_row;

  return session_row;
end;
$$;

revoke all on function public.create_device_activation_session(text, text, text, timestamptz) from public;
drop function if exists public.create_device_activation_session(text, text, text, timestamptz);

grant execute on function public.create_device_activation_session(text, text, text) to anon, authenticated;

-- ---------------------------------------------------------------------------
-- Fix approve_device_activation: bind approver to auth.uid()
-- ---------------------------------------------------------------------------
create or replace function public.approve_device_activation(
  activation_code text
)
returns public.device_activation_sessions
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

  select * into session_row
  from public.device_activation_sessions
  where code = upper(trim(activation_code))
    and status = 'PENDING'
    and expires_at > now()
  for update;

  if not found then
    raise exception 'Invalid or expired activation code';
  end if;

  update public.device_activation_sessions
  set status = 'APPROVED',
      user_id = approving_user_id
  where id = session_row.id
  returning * into session_row;

  return session_row;
end;
$$;

revoke all on function public.approve_device_activation(text, uuid) from public;
drop function if exists public.approve_device_activation(text, uuid);

grant execute on function public.approve_device_activation(text) to authenticated;
