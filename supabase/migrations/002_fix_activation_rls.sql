-- Fix TV activation inserts: default status + clearer RLS + anon grants

alter table public.device_activation_sessions
  alter column status set default 'PENDING';

drop policy if exists "Anyone can create activation session" on public.device_activation_sessions;

create policy "Anyone can create activation session"
on public.device_activation_sessions
for insert
to anon, authenticated
with check (
  user_id is null
  and status = 'PENDING'
);

grant select, insert on public.device_activation_sessions to anon, authenticated;
grant update on public.device_activation_sessions to authenticated;

-- RPC so TV can create sessions without direct insert RLS edge cases
create or replace function public.create_device_activation_session(
  activation_code text,
  activation_qr_token text,
  activation_device_name text default null,
  activation_expires_at timestamptz default (now() + interval '15 minutes')
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
    activation_expires_at
  )
  returning * into session_row;

  return session_row;
end;
$$;

grant execute on function public.create_device_activation_session(text, text, text, timestamptz) to anon, authenticated;
