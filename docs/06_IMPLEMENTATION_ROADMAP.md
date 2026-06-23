# IPTV Cinema — Implementation Roadmap

## Phase 0 — Decisions Before Coding

Decide these before writing code:

1. Is the app only Android TV or also tablets/phones later?
2. Will provider credentials be local-only or cloud-synced?
3. Will Supabase store only user data or also synced catalogs?
4. Will the app support demo mode?
5. Will the app be distributed through Google Play or direct APK?

Recommended answers:

```text
Android TV first.
Credentials local-only for MVP.
Supabase stores user/profile/favorites/settings/source metadata.
Room stores large IPTV catalogs.
Demo mode yes.
Google Play compatible, no bundled illegal streams.
```

## Phase 1 — Project Foundation

Tasks:

```text
1. Create native Android project.
2. Use Kotlin.
3. Enable Jetpack Compose.
4. Add Android TV manifest support.
5. Add Leanback launcher category.
6. Add TV banner asset.
7. Add Compose for TV dependency.
8. Add Navigation Compose.
9. Add Hilt.
10. Add Coil.
11. Add Room.
12. Add DataStore.
13. Add Retrofit/OkHttp.
14. Add Media3 ExoPlayer.
15. Add Supabase Kotlin.
```

Deliverable:

```text
App launches on Android TV emulator and physical device.
```

## Phase 2 — Design System

Tasks:

```text
1. Create CinemaColors.
2. Create CinemaTypography.
3. Create CinemaSpacing.
4. Create CinemaShapes.
5. Create CinemaScreen.
6. Create CinemaTopNav.
7. Create FocusableCinemaCard.
8. Create CinemaButton.
9. Create PosterCard.
10. Create ChannelTile.
11. Create ContentRail.
12. Create RemoteHintBar.
```

Deliverable:

```text
Reusable UI components match the generated IPTV Cinema style.
```

## Phase 3 — Fake Data UI Screens

Build screens with fake data first:

```text
1. Splash
2. Activation
3. Add Source Choice
4. Xtream Codes Form
5. M3U Form
6. Profile Selection
7. Home
8. Live TV
9. Movies
10. Series
11. Details
12. Player Overlay
13. Search
14. My List
15. Settings
16. Playlist Management
17. Parental Controls
18. Empty/Error States
```

Deliverable:

```text
Complete clickable TV prototype with fake data and correct focus behavior.
```

## Phase 4 — Navigation and State

Tasks:

```text
1. Add AppNavGraph.
2. Add route guards.
3. Add session state.
4. Add current profile state.
5. Add selected source state.
6. Add back navigation rules.
7. Add focus restoration per screen.
```

Route logic:

```text
Splash
→ no auth: Activation
→ auth but no source: Add Source
→ auth + source but no profile: Profile Selection
→ ready: Home
```

Deliverable:

```text
Real app flow works without backend data.
```

Status: **Complete**

Completed in Phase 4:

```text
1. AppNavGraph with full route wiring
2. Runtime SessionRouteGuard + startup routing
3. DataStore session state (auth, source, profile)
4. Profile selection with persisted currentProfileId
5. Source state surfaced in Settings and Playlist Management
6. Back navigation rules (splash, onboarding, main shell, detail screens)
7. Focus restoration with saved chip/section index per screen
```

Next: Phase 5 — Supabase Integration

## Phase 5 — Supabase Integration

Tasks:

```text
1. Create Supabase project.
2. Add Auth.
3. Add database tables.
4. Enable RLS.
5. Add Kotlin Supabase client.
6. Implement AuthRepository.
7. Implement DeviceActivationRepository.
8. Implement ProfilesRepository.
9. Implement FavoritesRepository.
10. Implement WatchHistoryRepository.
11. Implement UserSettingsRepository.
12. Implement PlaylistSourcesRepository.
```

Deliverable:

```text
User can sign in, select profile, save favorites, sync settings.
```

Status: **Complete**

Completed in Phase 5:

```text
1. Supabase migration with 7 tables, RLS, signup triggers, approve_device_activation RPC
2. BuildConfig + SupabaseModule + DTOs/mappers
3. AuthRepository + DeviceActivationRepository with TV polling + token exchange
4. ProfilesRepository + PlaylistSourcesRepository + LocalCredentialsStore
5. FavoritesRepository + WatchHistoryRepository + UserSettingsRepository
6. ParentalControlsRepository with per-profile persistence
7. Wired Activation, Splash, Profiles, Sources, Settings, My List, Details, Parental screens
8. Edge function exchange-activation-session + production activation companion at iptv.afifistudio.com
9. TV activation E2E verified: email/password + Google OAuth, QR ?activation= flow, auto sign-in
10. Server-side session expiry (migration 003), TV double-exchange fix, polished companion UI v3.1
```

Deliverable achieved:

```text
User can sign in on TV via device activation, select profile, save favorites, sync settings.
Companion at https://iptv.afifistudio.com links TV to Supabase account.
```

## Phase 6 — Xtream Codes Integration

Tasks:

```text
1. Create XtreamApi client.
2. Validate server URL.
3. Authenticate account.
4. Fetch account info.
5. Fetch live categories.
6. Fetch live streams.
7. Fetch VOD categories.
8. Fetch VOD streams.
9. Fetch series categories.
10. Fetch series.
11. Fetch XMLTV if available.
12. Normalize data.
13. Save to Room.
14. Display in Home, Live TV, Movies, Series.
```

Deliverable:

```text
User can connect Xtream Codes and browse real provider content.
```

Status: **Complete**

Completed in Phase 6:

```text
1. Retrofit XtreamApi + DTOs + URL/stream URL helpers + NetworkModule
2. Room catalog database (channels, movies, series, categories, programs, sync state)
3. XtreamNormalizer + CatalogEntityMapper (in CatalogUiMapper.kt) + CatalogUiMapper
4. XtreamRepository + XtreamSyncRepository with foreground sync on connect/re-sync
5. CatalogRepository with demo-mode fallback scoped to active source
6. Xtream form live validation checklist + debug BuildConfig test credential prefill
7. Home/Live TV/Movies/Series ViewModels wired to Room catalog
8. Minimal XmltvParser + EPG upsert during sync
9. Playlist management shows channel/movie/series counts, last synced, manual re-sync
10. Verified against servx.pro test provider (auth + categories API smoke test; not full E2E playback)
```

### Phase 6 scope boundary

**In scope (done):**

```text
Xtream API client, URL validation, auth, full catalog sync (live/VOD/series metadata + EPG upsert),
Room cache, browse rails (Home, Live TV, Movies, Series), playlist sync metadata,
connect-time validation checklist, debug credential prefill.
```

**Explicitly deferred (later phases):**

```text
Details from Room catalog
Search over Room catalog
Lazy series episode fetch (get_series_info)
Full EPG guide grid
Playback (Media3)
WorkManager periodic refresh
```

### Known gaps / follow-ups before MVP milestone

These are tracked items, not Phase 6 blockers:

```text
1. Browse → Details ID mismatch — resolved in Phase 10 finalization (contentId on series posters)
2. Series episodes — on-demand fetch via EpisodeCatalogRepository; top-5 prefetch after Xtream sync
3. Source delete Room purge — resolved in Phase 10 finalization (CatalogDaoFacade.purgeSource)
4. Channel now playing — resolved in Phase 10 finalization (PlaybackSessionTracker + guide/tile highlight)
5. observeSyncState UI — resolved in Phase 10 finalization (Home sync banner)
6. No Xtream unit tests yet — partial (XtreamNormalizerTest, XtreamSkipRefetchTest)
```

### Implementation map

```text
core/xtream/        — XtreamApi, XtreamRepository, XtreamSyncRepository, XtreamNormalizer
core/database/      — CatalogEntities, CatalogDaos, CatalogDaoFacade
core/data/repository/CatalogRepository.kt
features/sources/   — connect form + sync UX
features/home|livetv|movies|series/ — browse ViewModels
core/epg/XmltvParser.kt — minimal EPG during sync
```

## Phase 7 — M3U Integration

Tasks:

```text
1. Create M3U downloader.
2. Create M3U parser.
3. Support common M3U attributes.
4. Add optional headers/user-agent.
5. Normalize entries into Channel model.
6. Add XMLTV EPG parser.
7. Match EPG to channels.
8. Save to Room.
9. Display channels in Live TV and Guide.
```

Deliverable:

```text
User can import M3U playlist and watch channels.
```

Status: **Complete**

Completed in Phase 7:

```text
1. core/m3u/ package: M3uParser, M3uDownloader, M3uNormalizer, M3uSyncRepository
2. M3uCredentials extended with referer + customHeaders; LocalCredentialsStore updated
3. SourceViewModel sync on import + M3uConnectUiState checklist + resync branch for M3U
4. M3uFormScreen live import status panel; Referer/Headers wired through
5. Room catalog via replaceLiveCatalog; optional EPG via reused XmltvParser
6. Live TV reads M3U channels from CatalogRepository (no screen changes needed)
7. Unit tests: M3uParserTest, M3uNormalizerTest, M3uLiveTvPipelineTest
```

Scope boundary: browse + EPG data only; playback deferred to Phase 8.

### Implementation map

```text
core/m3u/           — M3uParser, M3uDownloader, M3uNormalizer, M3uSyncRepository
features/sources/   — M3uFormScreen connect state, saveM3uSource sync flow
core/epg/XmltvParser.kt — reused for optional M3U EPG URL
```

## Phase 8 — Player

Tasks:

```text
1. Create PlayerManager.
2. Add Media3 ExoPlayer.
3. Build PlayerScreen.
4. Add fullscreen playback.
5. Add custom overlay.
6. Add live stream support.
7. Add HLS support.
8. Add error handling.
9. Add buffering UI.
10. Add audio track selector.
11. Add subtitle selector.
12. Save playback progress.
13. Resume VOD playback.
```

Deliverable:

```text
Stable playback from Xtream and M3U sources.
```

Status: **Complete**

Completed in Phase 8:

```text
1. PlayerManager + ExoPlayer with IPTV-tuned buffer and M3U/Xtream stream resolution
2. PlayerScreen with TV overlay, seek bar, auto-hide controls, track pickers
3. Live + VOD + episode playback from Room catalog via PlaybackRepository
4. Lazy Xtream episode fetch (EpisodeCatalogRepository) for playback without visiting details first
5. Watch history save/resume (Supabase) with continue-watching rail on Home and My List
6. Series continuity: Skip Next, Up Next rail, settings-driven autoplay countdown
7. Live channel zapping (Ch+/−) with EPG metadata on overlay
8. Dynamic quality badge, context-aware error recovery, RemoteHintBar, rebuffer UX
9. Unit tests: WatchHistoryResumePolicy, EpisodeSequenceHelper
```

Next: Phase 9 — EPG Performance

Tasks:

```text
1. Parse XMLTV in background.
2. Store EPG in Room.
3. Render visible time window only.
4. Add current time indicator.
5. Add category filters.
6. Add +24h / -24h navigation.
7. Add focused program details.
```

Deliverable:

```text
Smooth TV guide navigation without lag.
```

Status: **Complete**

Completed in Phase 9:

```text
1. EpgSyncRepository — background XMLTV parse on Dispatchers.Default/IO after catalog sync completes
2. Room programs table with window index (migration v3) and deleteOlderThan trim after upsert
3. ProgramGuideGrid rebuilt with LazyColumn, epoch-based cell positioning, visible time window only
4. Live current-time indicator driven by real clock (updates every 60s)
5. Category filters unchanged; guide respects filtered channel list from Room
6. PageUp/PageDown ±24h guide window shifts on Live TV
7. Focus-driven LivePreviewCard with program title, description, times, and progress
8. Batch current-program lookup for On Now / channel browse (observeChannelBrowse)
9. Unit tests: GuideLayoutHelperTest, XmltvParserTest, EpgWindowMathTest
```

Deliverable achieved:

```text
Smooth TV guide navigation without lag — lazy grid, decoupled EPG sync, real Room EPG data.
```

Next: Phase 10 — Polish (finalized)

## Phase 10 — Polish

Status: **Complete (finalized)**

Deliverable achieved:

```text
App feels production-ready.
```

### Completed in Phase 10

```text
1. Skeleton loading — SkeletonComponents + CatalogStateContent (Home, Movies, Series, Live TV, Search, Details, Profiles, My List, Sources)
2. Empty/error states — CatalogStateContent on browse + Search; EmptyState on Profile selection errors
3. Stream error states — Player ErrorState; catalog ErrorState via CatalogStateContent
4. Expired account state — ExpiredAccountState + source status routing on all browse tabs + Search
5. Invalid playlist state — InvalidPlaylistState for failed M3U sources
6. Parental PIN flow — PinEntryDialog, pin_hash in Supabase, set/change/remove PIN
7. Profile restrictions — ParentalGate category filters + RatingPolicy max-rating enforcement in browse, search, and details playback
8. Image fallback system — CinemaAsyncImage with gradient + initials; loading skeleton while fetching
9. TV remote hints — RemoteHintBar on all main-shell tabs
10. Coil tuning — CoilModule (memory/disk cache, crossfade)
11. Correctness — series contentId on posters; purgeSource on delete; no title-matching navigation fallbacks
12. Sync/playback UX — Home sync banner; now-playing channel highlight in guide and live rail; top-5 episode prefetch after series sync
13. Unit tests — ParentalGateTest, PinHasherTest, RatingPolicyTest, SyncStatusFormatterTest (+ existing suite); ./gradlew testDebugUnitTest passes
```

### Scope boundary

**In scope (done):** production UX states, real Room search, parental PIN + max-rating enforcement, image fallbacks, source-health recovery, sync banner, now-playing highlight, catalog purge on source delete.

**Deferred to post-MVP / release prep:**

```text
WorkManager periodic catalog refresh
Full Xtream integration test suite
Launcher assets and Play Store release pipeline
```

### Resolved in Phase 10 finalization (Phase 6 carry-over)

```text
Browse → Details ID wiring (CatalogSeries.toPosterCardData contentId)
Source delete → Room purge (CatalogDaoFacade.purgeSource)
Search state parity (CatalogStateContent + sourceStatus)
Channel now-playing (PlaybackSessionTracker)
observeSyncState UI (SyncStatusBanner on Home)
```

### Manual TV verification checklist

Run on Android TV emulator and physical device before MVP sign-off:

| Scenario | Pass criteria |
|----------|---------------|
| Slow network (3G profile) | Skeletons visible ≥2s, no ANR, image fallbacks on failed loads |
| Expired Xtream | Browse/Search show ExpiredAccountState; recovery via Manage Sources |
| Bad M3U URL | InvalidPlaylistState on browse + source management |
| Parental PIN | Set PIN → blocked categories hidden → locked settings require PIN → max rating hides restricted titles |
| D-pad only | All main tabs, search, details, player error recovery, back stack correct |
| Delete source | Room catalog purged; no ghost content on other tabs |
| Series browse | Poster click opens correct details by ID |
| Live playback | Current channel highlighted in guide; sync banner updates during re-sync |

Automated gate: `./gradlew testDebugUnitTest assembleDebug` (passing).

### Implementation map

```text
core/design/components/  — SkeletonComponents, CatalogStateContent, CinemaAsyncImage, SyncStatusBanner
core/parental/           — ParentalGate, PinHasher, RatingPolicy
core/player/             — PlaybackSessionTracker
core/database/           — CatalogDaoFacade.purgeSource
features/search/         — SearchViewModel + CatalogStateContent
features/home/           — sync banner + now-playing live rail
features/details/        — detail skeletons + playbackBlocked by max rating
```

Next: **MVP Milestone** validation and Sprint 7 release preparation

## MVP Milestone

The first real milestone should be:

```text
Launch app
→ Sign in
→ Add Xtream Codes source
→ Choose profile
→ View Home
→ Open Live TV
→ Play one channel
→ Add channel to favorites
→ Reopen app and resume same profile
```

## Suggested Sprint Plan

### Sprint 1 — App Foundation

```text
Project setup
TV manifest
Navigation
Design system
Reusable focus components
```

### Sprint 2 — UI Prototype

```text
Splash
Activation
Profile Selection
Home
Movies
Details
Player mock
Settings
```

### Sprint 3 — Supabase

```text
Auth
Profiles
Favorites
Watch history
Settings
Playlist source metadata
```

### Sprint 4 — Xtream

```text
Xtream login
Sync categories
Sync live channels
Sync VOD
Room cache
```

### Sprint 5 — Playback

```text
Media3 player
Live channel playback
VOD playback
Overlay controls
Error states
```

### Sprint 6 — M3U + EPG

```text
M3U parser
XMLTV parser
Guide grid
EPG matching
```

### Sprint 7 — Production Polish

```text
Parental controls
Performance
Testing
Launcher assets
Release preparation
```

## Definition of Done

A screen is done only when:

```text
1. It works with D-pad.
2. Focus state is obvious.
3. Back button behavior is correct.
4. Loading state exists.
5. Empty state exists.
6. Error state exists.
7. Text is readable from TV distance.
8. It handles slow data.
9. It handles missing images.
10. It does not crash from bad provider data.
```

## Critical Risks

### 1. Illegal IPTV Perception

Mitigation:

```text
No bundled channels.
Clear user-provided-source messaging.
No copyrighted brand/channel logos unless licensed.
```

### 2. Bad Provider Data

Mitigation:

```text
Normalize defensively.
Never trust provider responses.
Support missing logos, bad categories, invalid URLs.
```

### 3. EPG Performance

Mitigation:

```text
Limit visible range.
Lazy render.
Cache parsed XMLTV.
Trim old EPG.
```

### 4. Playback Instability

Mitigation:

```text
Media3.
Timeout handling.
Retry.
Alternative stream option.
Clear error states.
```

### 5. TV Focus Bugs

Mitigation:

```text
Build focus components first.
Test every screen with D-pad only.
Avoid touch assumptions.
```
