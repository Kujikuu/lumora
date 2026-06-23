# IPTV Cinema — Technical Architecture

## Stack

| Layer | Technology |
|---|---|
| App | Kotlin native Android TV |
| UI | Jetpack Compose for TV |
| Navigation | Navigation Compose |
| Playback | Media3 ExoPlayer |
| Backend | Supabase |
| Database | Supabase Postgres |
| Auth | Supabase Auth |
| Realtime | Supabase Realtime where useful |
| Local Storage | Room + DataStore |
| Images | Coil |
| Networking | Retrofit + OkHttp |
| Serialization | KotlinX Serialization |
| Catalog Sync | Foreground coroutine sync on connect/re-sync (WorkManager planned for periodic refresh) |
| Dependency Injection | Hilt |

## Architecture Pattern

Use:

```text
MVVM + Repository + Use Cases
```

Recommended package structure:

```text
com.iptvcinema.tv
  app/
    MainActivity.kt
    IptvCinemaApp.kt

  core/
    design/
    navigation/
    player/
    database/
    datastore/
    network/
    supabase/
    xtream/
    m3u/
    epg/
    model/
    util/

  features/
    splash/
    activation/
    profiles/
    home/
    livetv/
    guide/
    movies/
    series/
    details/
    player/
    search/
    mylist/
    settings/
    sources/
    parental/
```

## High-Level Data Flow

```text
Xtream Codes / M3U Source
        ↓
Source Repository
        ↓
Normalize into local app models
        ↓
Room Cache
        ↓
ViewModel StateFlow
        ↓
Compose for TV UI
        ↓
User selects item
        ↓
Media3 ExoPlayer
```

Supabase sits beside this:

```text
Supabase Auth
Supabase Profiles
Supabase Sources Metadata
Supabase Favorites
Supabase Watch History
Supabase Settings
Supabase Device Activation
```

## Why Normalize IPTV Data

Xtream Codes and M3U expose different formats.

The app should not let screens depend directly on raw provider responses.

Convert all sources into shared models:

```text
ContentItem
Channel
Movie
Series
Episode
Program
Category
PlaylistSource
```

This lets the UI stay stable even when source format changes.

## Supabase Responsibilities

Supabase should store user/account state, not necessarily every IPTV stream.

Recommended Supabase responsibilities:

- Auth users.
- Device activation sessions.
- Profiles.
- Playlist source metadata.
- Encrypted source credentials if needed.
- Favorites.
- Watch history.
- Playback progress.
- User preferences.
- Parental controls.
- Support/account data.
- Optional curated metadata.

Avoid blindly storing huge provider catalogs in Supabase at first.

Large channel/VOD catalogs should be cached locally on-device unless multi-device sync becomes required.

## Local Database Responsibilities

Room should store:

- Synced channels.
- Synced categories.
- Synced movies.
- Synced series.
- Synced episodes (lazy per-series via get_series_info — not bulk-synced in Phase 6).
- EPG programs.
- Recently watched cache.
- Search index cache (planned — Search still uses fake data).
- Source sync timestamps.

DataStore should store:

- Current profile ID.
- Last selected source.
- Playback preferences.
- UI preferences.
- Lightweight session flags.

## Backend Source Security

IPTV source credentials are sensitive.

Options:

### Option A — Local-only credentials

Store Xtream/M3U credentials encrypted on device.

Pros:

- Better privacy.
- Less backend liability.

Cons:

- Harder multi-device sync.

### Option B — Supabase encrypted credentials

Store encrypted credentials in Supabase.

Pros:

- Multi-device sync.
- Easier account restore.

Cons:

- Higher security responsibility.

Recommended MVP:

```text
Store source credentials locally first.
Store only source metadata in Supabase.
```

Later:

```text
Add optional encrypted cloud sync.
```

## Xtream Codes Integration

Typical Xtream Codes API patterns:

```text
/player_api.php?username={username}&password={password}
/player_api.php?username={username}&password={password}&action=get_live_categories
/player_api.php?username={username}&password={password}&action=get_live_streams
/player_api.php?username={username}&password={password}&action=get_vod_categories
/player_api.php?username={username}&password={password}&action=get_vod_streams
/player_api.php?username={username}&password={password}&action=get_series_categories
/player_api.php?username={username}&password={password}&action=get_series
/xmltv.php?username={username}&password={password}
```

Do not hardcode assumptions. Providers vary.

Create:

```kotlin
interface XtreamApi {
    suspend fun authenticate(): XtreamAccountInfo
    suspend fun getLiveCategories(): List<XtreamCategory>
    suspend fun getLiveStreams(): List<XtreamLiveStream>
    suspend fun getVodCategories(): List<XtreamCategory>
    suspend fun getVodStreams(): List<XtreamVodStream>
    suspend fun getSeriesCategories(): List<XtreamCategory>
    suspend fun getSeries(): List<XtreamSeries>
    suspend fun getXmltv(): String
}
```

### Phase 6 implementation notes

Actual implementation (Phase 6 complete):

```text
XtreamApi.kt          — Retrofit interface; includes get_series_info (not shown in conceptual interface above)
XtreamSyncRepository  — syncSource() orchestrates 9 steps with progress reporting
XmltvParser           — minimal EPG parse during sync; failures are non-fatal
Episodes              — lazy-loaded on demand via get_series_info, not bulk-synced
Browse screens        — Home, Live TV, Movies, Series, Search read from CatalogRepository → Room
Details / Player      — Room catalog path; demo mode falls back to FakeDataProvider for cast/rails
Phase 10 additions    — CatalogDaoFacade.purgeSource, RatingPolicy (max_rating), PlaybackSessionTracker, SyncStatusBanner
```

Package paths:

```text
app/src/main/java/com/iptvcinema/tv/core/xtream/
app/src/main/java/com/iptvcinema/tv/core/database/
app/src/main/java/com/iptvcinema/tv/core/data/repository/CatalogRepository.kt
app/src/main/java/com/iptvcinema/tv/features/sources/
app/src/main/java/com/iptvcinema/tv/features/home|livetv|movies|series/
app/src/main/java/com/iptvcinema/tv/core/epg/XmltvParser.kt
```

## M3U Integration

M3U parser should support:

```text
#EXTM3U
#EXTINF
tvg-id
tvg-name
tvg-logo
group-title
catchup
catchup-source
radio
```

Example:

```text
#EXTINF:-1 tvg-id="channel.id" tvg-name="Channel Name" tvg-logo="https://logo.png" group-title="News",Channel Name
https://stream-url.m3u8
```

Normalize into:

```kotlin
data class M3uEntry(
    val name: String,
    val url: String,
    val tvgId: String?,
    val tvgName: String?,
    val logo: String?,
    val group: String?,
    val attributes: Map<String, String>
)
```

## EPG / XMLTV

EPG sync should:

1. Download XMLTV.
2. Parse channels.
3. Parse programs.
4. Match programs by `tvg-id`, `channel id`, or normalized name.
5. Store in Room.
6. Trim old EPG data.

EPG performance warning:

```text
Do not render thousands of program cells at once.
Use lazy layouts, paging, and visible time windows.
```

## Playback Layer

Use Media3 ExoPlayer for:

- HLS `.m3u8`
- DASH if needed
- MP4
- Live streams
- Subtitles
- Audio tracks
- Resume positions

Create a central player controller:

```text
core/player/
  PlayerManager.kt
  PlayerState.kt
  PlayerCommand.kt
  PlaybackRepository.kt
```

Player state should include:

```kotlin
data class PlayerUiState(
    val title: String,
    val streamUrl: String,
    val isLive: Boolean,
    val isBuffering: Boolean,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long?,
    val errorMessage: String?,
    val audioTracks: List<TrackOption>,
    val subtitleTracks: List<TrackOption>,
    val selectedQuality: String?
)
```

## App Startup Logic

```text
MainActivity
→ AppNavGraph
→ SplashViewModel checks:
    - Supabase session
    - Local source exists
    - Profiles exist
    - Last sync status
→ Route decision:
    - no session: Activation
    - session but no source: Add Source
    - session and source: Choose Profile
```

## Error Handling

You need first-class error states for:

- Invalid Xtream credentials.
- Server unreachable.
- M3U URL unavailable.
- Malformed M3U.
- EPG parse failure.
- Stream timeout.
- Unsupported codec.
- Geo-blocked stream.
- Empty category.
- Expired IPTV account.

Never crash because a provider sends bad data.

## Recommended Build Rule

Build UI against fake data first.

Then wire repositories.

Then wire real providers.

Do not connect IPTV sources before the UI shell, navigation, and player are stable.
