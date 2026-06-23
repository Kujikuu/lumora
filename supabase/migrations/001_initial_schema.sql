-- IPTV Cinema initial schema (Phase 5)

-- ---------------------------------------------------------------------------
-- Helper: updated_at trigger
-- ---------------------------------------------------------------------------
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------------
create table public.profiles (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  type text not null check (type in ('MAIN', 'FAMILY', 'KIDS', 'GUEST')),
  avatar_url text,
  parental_pin_enabled boolean not null default false,
  max_rating text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index profiles_user_id_idx on public.profiles(user_id);

create trigger profiles_set_updated_at
before update on public.profiles
for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- device_activation_sessions
-- ---------------------------------------------------------------------------
create table public.device_activation_sessions (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  qr_token text not null unique,
  status text not null check (status in ('PENDING', 'APPROVED', 'EXPIRED')),
  user_id uuid references auth.users(id) on delete cascade,
  device_name text,
  expires_at timestamptz not null,
  created_at timestamptz not null default now()
);

create index device_activation_sessions_code_idx on public.device_activation_sessions(code);
create index device_activation_sessions_qr_token_idx on public.device_activation_sessions(qr_token);
create index device_activation_sessions_status_idx on public.device_activation_sessions(status);

-- ---------------------------------------------------------------------------
-- playlist_sources
-- ---------------------------------------------------------------------------
create table public.playlist_sources (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  type text not null check (type in ('XTREAM_CODES', 'M3U', 'DEMO')),
  server_url text,
  playlist_url text,
  epg_url text,
  encrypted_credentials jsonb,
  is_active boolean not null default true,
  status text not null default 'ACTIVE',
  last_synced_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index playlist_sources_user_id_idx on public.playlist_sources(user_id);

create trigger playlist_sources_set_updated_at
before update on public.playlist_sources
for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- favorites
-- ---------------------------------------------------------------------------
create table public.favorites (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  profile_id uuid not null references public.profiles(id) on delete cascade,
  source_id uuid references public.playlist_sources(id) on delete cascade,
  content_id text not null,
  content_type text not null check (content_type in ('CHANNEL', 'MOVIE', 'SERIES', 'EPISODE')),
  title text not null,
  poster_url text,
  created_at timestamptz not null default now(),
  unique(profile_id, content_id, content_type)
);

create index favorites_profile_id_idx on public.favorites(profile_id);

-- ---------------------------------------------------------------------------
-- watch_history
-- ---------------------------------------------------------------------------
create table public.watch_history (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  profile_id uuid not null references public.profiles(id) on delete cascade,
  source_id uuid references public.playlist_sources(id) on delete cascade,
  content_id text not null,
  content_type text not null check (content_type in ('CHANNEL', 'MOVIE', 'EPISODE')),
  title text not null,
  poster_url text,
  position_ms bigint not null default 0,
  duration_ms bigint,
  watched_percentage numeric,
  last_watched_at timestamptz not null default now(),
  unique(profile_id, content_id, content_type)
);

create index watch_history_profile_id_idx on public.watch_history(profile_id);
create index watch_history_last_watched_at_idx on public.watch_history(last_watched_at desc);

-- ---------------------------------------------------------------------------
-- user_settings
-- ---------------------------------------------------------------------------
create table public.user_settings (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  default_audio_language text default 'en',
  default_subtitle_language text,
  subtitles_enabled boolean not null default false,
  autoplay_next_episode boolean not null default true,
  continue_watching_enabled boolean not null default true,
  skip_intro_enabled boolean not null default false,
  streaming_quality text not null default 'AUTO',
  theme text not null default 'DARK_CINEMA',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(user_id)
);

create trigger user_settings_set_updated_at
before update on public.user_settings
for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- parental_controls
-- ---------------------------------------------------------------------------
create table public.parental_controls (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  profile_id uuid not null references public.profiles(id) on delete cascade,
  pin_hash text,
  hide_adult_categories boolean not null default true,
  lock_playlist_settings boolean not null default false,
  lock_live_categories boolean not null default false,
  max_rating text,
  blocked_categories text[] not null default '{}',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(profile_id)
);

create trigger parental_controls_set_updated_at
before update on public.parental_controls
for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- Default profile + settings on signup
-- ---------------------------------------------------------------------------
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  new_profile_id uuid;
begin
  insert into public.profiles (user_id, name, type)
  values (new.id, 'Main', 'MAIN')
  returning id into new_profile_id;

  insert into public.user_settings (user_id)
  values (new.id);

  insert into public.parental_controls (user_id, profile_id)
  values (new.id, new_profile_id);

  return new;
end;
$$;

create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------------
-- Row Level Security
-- ---------------------------------------------------------------------------
alter table public.profiles enable row level security;
alter table public.device_activation_sessions enable row level security;
alter table public.playlist_sources enable row level security;
alter table public.favorites enable row level security;
alter table public.watch_history enable row level security;
alter table public.user_settings enable row level security;
alter table public.parental_controls enable row level security;

-- profiles
create policy "Users manage own profiles"
on public.profiles for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

-- playlist_sources
create policy "Users manage own playlist sources"
on public.playlist_sources for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

-- favorites
create policy "Users manage own favorites"
on public.favorites for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

-- watch_history
create policy "Users manage own watch history"
on public.watch_history for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

-- user_settings
create policy "Users manage own settings"
on public.user_settings for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

-- parental_controls
create policy "Users manage own parental controls"
on public.parental_controls for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

-- device_activation_sessions: anon can create and read pending sessions
create policy "Anyone can create activation session"
on public.device_activation_sessions for insert
with check (status = 'PENDING' and user_id is null);

create policy "Anyone can read activation session by code or token"
on public.device_activation_sessions for select
using (true);

create policy "Authenticated users can approve activation session"
on public.device_activation_sessions for update
using (auth.uid() is not null)
with check (
  auth.uid() is not null
  and status in ('APPROVED', 'EXPIRED')
);

-- RPC: approve device activation (called from companion after user signs in)
create or replace function public.approve_device_activation(
  activation_code text,
  approving_user_id uuid default auth.uid()
)
returns public.device_activation_sessions
language plpgsql
security definer
set search_path = public
as $$
declare
  session_row public.device_activation_sessions;
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

grant execute on function public.approve_device_activation(text, uuid) to authenticated;
