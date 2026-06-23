# IPTV Cinema — Data Models and Supabase Schema

## Data Strategy

Use two data layers:

```text
Supabase: user/account/profile/source metadata/sync state
Room: large local IPTV catalog, EPG, cached content
```

Do not put every synced channel/movie/episode into Supabase during MVP unless you need multi-device catalog sync.

## Kotlin Core Models

### PlaylistSource

```kotlin
data class PlaylistSource(
    val id: String,
    val userId: String,
    val name: String,
    val type: SourceType,
    val serverUrl: String?,
    val playlistUrl: String?,
    val epgUrl: String?,
    val isActive: Boolean,
    val lastSyncedAt: Instant?,
    val status: SourceStatus
)

enum class SourceType {
    XTREAM_CODES,
    M3U
}

enum class SourceStatus {
    ACTIVE,
    SYNCING,
    NEEDS_ATTENTION,
    EXPIRED,
    FAILED
}
```

### Profile

```kotlin
data class UserProfile(
    val id: String,
    val userId: String,
    val name: String,
    val avatarUrl: String?,
    val type: ProfileType,
    val parentalPinEnabled: Boolean,
    val maxRating: ContentRating?
)

enum class ProfileType {
    MAIN,
    FAMILY,
    KIDS,
    GUEST
}
```

### Channel

```kotlin
data class Channel(
    val id: String,
    val sourceId: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val categoryId: String?,
    val categoryName: String?,
    val tvgId: String?,
    val isAdult: Boolean,
    val isFavorite: Boolean,
    val sortOrder: Int?
)
```

### Program

```kotlin
data class Program(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String?,
    val startAt: Instant,
    val endAt: Instant,
    val category: String?,
    val iconUrl: String?
)
```

### Movie

```kotlin
data class Movie(
    val id: String,
    val sourceId: String,
    val title: String,
    val streamUrl: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val categoryId: String?,
    val categoryName: String?,
    val year: Int?,
    val durationMinutes: Int?,
    val rating: String?,
    val plot: String?,
    val cast: List<String>,
    val isFavorite: Boolean,
    val progressMs: Long
)
```

### Series

```kotlin
data class Series(
    val id: String,
    val sourceId: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val categoryId: String?,
    val categoryName: String?,
    val plot: String?,
    val rating: String?,
    val seasons: List<Season>
)

data class Season(
    val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodes: List<Episode>
)

data class Episode(
    val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val streamUrl: String,
    val durationMinutes: Int?,
    val plot: String?,
    val thumbnailUrl: String?,
    val progressMs: Long
)
```

### PlaybackProgress

```kotlin
data class PlaybackProgress(
    val id: String,
    val userId: String,
    val profileId: String,
    val contentId: String,
    val contentType: ContentType,
    val positionMs: Long,
    val durationMs: Long?,
    val updatedAt: Instant
)

enum class ContentType {
    CHANNEL,
    MOVIE,
    SERIES_EPISODE
}
```

## Supabase Tables

### profiles

```sql
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
```

### device_activation_sessions

```sql
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
```

### playlist_sources

```sql
create table public.playlist_sources (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  type text not null check (type in ('XTREAM_CODES', 'M3U')),
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
```

### favorites

```sql
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
```

### watch_history

```sql
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
```

### user_settings

```sql
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
```

### parental_controls

```sql
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
```

**Client enforcement (Phase 10):** `max_rating` is enforced on-device via `RatingPolicy` + `ParentalGate.isContentBlocked()` in browse, search, and details playback. Unrated provider metadata is allowed; unrecognized ratings are treated permissively.

## RLS Policies

Enable RLS on every public user table.

```sql
alter table public.profiles enable row level security;
alter table public.playlist_sources enable row level security;
alter table public.favorites enable row level security;
alter table public.watch_history enable row level security;
alter table public.user_settings enable row level security;
alter table public.parental_controls enable row level security;
```

Basic owner policy:

```sql
create policy "Users can manage their own profiles"
on public.profiles
for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);
```

Repeat for user-owned tables.

## Room Tables

Room should cache high-volume data.

Recommended local entities:

```text
LocalChannelEntity          — implemented
LocalCategoryEntity         — implemented
LocalProgramEntity          — implemented
LocalMovieEntity            — implemented
LocalSeriesEntity           — implemented
LocalEpisodeEntity          — implemented (table exists; episodes not bulk-synced in Phase 6)
LocalSearchIndexEntity      — planned (not yet implemented)
LocalSourceSyncStateEntity  — implemented
```

## Search Strategy

MVP:

```text
Search local Room cache using LIKE queries and normalized text.
```

Later:

```text
Add FTS virtual table for fast local search.
```

Search across:

- Channel name.
- Category name.
- Movie title.
- Series title.
- Episode title.
- Program title.

## Sync Strategy

### Xtream Sync

```text
1. Validate account.
2. Fetch categories.
3. Fetch live streams.
4. Fetch VOD streams.
5. Fetch series metadata (episodes fetched lazily via get_series_info when Details opens — not bulk-synced).
6. Fetch EPG.
7. Normalize.
8. Upsert Room entities.
9. Update Supabase playlist_sources.last_synced_at.
```

### M3U Sync

```text
1. Download playlist.
2. Parse M3U entries.
3. Normalize channels.
4. Download EPG if provided.
5. Match EPG to channels.
6. Upsert Room entities.
7. Update sync status.
```

## Sync Failure States

Aspirational doc states (full granularity):

```text
ACTIVE
SYNCING
NEEDS_ATTENTION
EXPIRED
INVALID_CREDENTIALS
EPG_FAILED
PLAYLIST_FAILED
SERVER_UNREACHABLE
```

Phase 6 implements a 5-value `SourceStatus` enum. Mapping:

| Doc state | Implemented as |
|---|---|
| ACTIVE | ACTIVE |
| SYNCING | SYNCING |
| NEEDS_ATTENTION | NEEDS_ATTENTION |
| EXPIRED | EXPIRED |
| INVALID_CREDENTIALS | FAILED (auth error) |
| SERVER_UNREACHABLE | NEEDS_ATTENTION or FAILED |
| EPG_FAILED | Sync continues; epgAvailable = false in sync state |
| PLAYLIST_FAILED | FAILED |

Granular doc states above are aspirational for future phases.

## Data Privacy Rule

Credentials should be handled carefully.

MVP recommendation:

```text
Store provider credentials locally using Android encrypted storage.
Store only non-sensitive source metadata in Supabase.
```

Only use Supabase encrypted credentials if multi-device sync is required.
