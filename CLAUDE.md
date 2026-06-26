# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Native **Android TV** IPTV player (Kotlin + Jetpack Compose for TV + Media3 ExoPlayer). Supabase backend handles auth, profiles, favorites, watch history, settings, and playlist source metadata. Room caches large IPTV catalogs on-device. Supports Xtream Codes, M3U playlists, EPG/XMLTV, demo mode. Distributed as a Leanback launcher app.

Package: `com.iptvcinema.tv` · `minSdk=26` · `compileSdk/targetSdk=35` · JVM 17 · Hilt + KSP.

## Commands

```bash
# Build / install on a connected TV/device/emulator (Gradle wrapper, AGP 8.8.2, Gradle 8.11.1)
./gradlew :app:assembleDebug
./gradlew :app:installDebug

# JVM unit tests (no device required) — the only tests that exist
./gradlew :app:testDebugUnitTest
./gradlew :app:testDebugUnitTest --tests "com.iptvcinema.tv.core.parental.RatingPolicyTest"
./gradlew :app:testDebugUnitTest --tests "*Xtream*"   # glob on class name

# Lint / cleanup
./gradlew :app:lintDebug
./gradlew clean

# Supabase (backend lives in ./supabase — applied manually, no CI)
supabase db push
supabase functions deploy exchange-activation-session
```

There are no instrumented/E2E tests. `androidTest` deps are wired but unused. Kotlin backticks in test names mean glob filters need quoted patterns.

## Required local setup

`local.properties` (git-ignored) is loaded by `app/build.gradle.kts` and injected as `BuildConfig` fields. Copy `local.properties.example`. Empty `SUPABASE_URL` triggers a local-only fallback (Phase 4 behavior) so UI still works without a backend. `local.properties` also points `sdk.dir` at the Android SDK.

## Architecture

`MVVM + Repository + Use Cases`, single `:app` Gradle module. Three top-level packages under `com.iptvcinema.tv`:

- **`app/`** — `IptvCinemaApp` (`@HiltAndroidApp`, also supplies the Coil `ImageLoader`), `MainActivity` (landscape, Leanback launcher, installs splash screen, hosts `CinemaTheme { AppNavGraph() }`).
- **`core/`** — everything reusable: `database`, `datastore`, `data/{repository, fake, local, mapper}`, `supabase`, `xtream`, `m3u`, `epg`, `player`, `parental`, `catalog`, `network`, `navigation`, `design`, `model`, `platform`, `util`, `di`.
- **`features/`** — one folder per screen: `splash`, `activation`, `profiles`, `sources`, `home`, `livetv`, `movies`, `series`, `details`, `guide`, `player`, `search`, `mylist`, `settings`, `parental`, `states`. Each typically has a `*Screen.kt` (Compose) and a `*ViewModel.kt`.

### Data flow

```
Xtream / M3U / Demo source
   → XtreamSyncRepository / M3uSyncRepository / FakeDataProvider
   → normalizers (core/xtream, core/m3u, core/epg)
   → CatalogDaoFacade.replace{Live,Vod,Series,Programs}(...)  [Room, version 5]
   → CatalogRepository  (domain queries, Flow-based)
   → Feature ViewModel (StateFlow)
   → Compose for TV UI
   → Media3 ExoPlayer (core/player/PlayerManager)
```

All IPTV provider formats normalize into the shared models in `core/model/catalog/CatalogModels.kt` (`CatalogChannel`, `CatalogMovie`, `CatalogSeries`, `CatalogEpisode`, etc.) so screens never depend on raw provider DTOs.

### Session state machine (gates every screen)

`core/datastore/AppSessionState.kt` is the source of truth. `resolveStartupDestination()` picks the splash target; `meetsRequirement()` + `redirectRouteFor()` are enforced by `SessionRouteGuard` inside each `composable(...)` in `AppNavGraph.kt`. Requirements escalate: `None → Authenticated → HasSource → Ready`. Adding a new gated screen means wrapping it in `SessionRouteGuard(requirement = SessionRequirement.Ready)`.

### DI

Hilt modules live under `core/*/di/`. Repositories are bound to interfaces in `core/data/repository/di/RepositoryModule.kt`; the Supabase implementations live in `core/data/repository/supabase/` and there is a local credentials store at `core/data/local/LocalCredentialsStore.kt` (uses `androidx.security.crypto`). Source credentials are local-only by design — Supabase stores only source **metadata**.

### Catalog sync (`core/xtream/XtreamSyncRepository.kt`)

9-step pipeline (`XtreamSyncStep`) reported via `StateFlow`: validate URL → authenticate → live categories → live streams → VOD categories → VOD streams → series categories → series → watched-series episodes → EPG. `XmltvParser` failures are non-fatal. Episodes are **lazy** — fetched on demand via `get_series_info`, never bulk-synced. `CatalogDaoFacade.purgeSource()` wipes all rows for a source on delete.

### Playback (`core/player/`)

`PlayerManager` + `PlayerState` + `PlayerCommand` wrap Media3. `PlaybackSessionTracker` + `WatchHistoryResumePolicy` persist progress. `EpisodeSequenceHelper` / `NextEpisodeResolver` handle series continuity and channel zapping. `features/player/PlayerKeyHandler.kt` maps remote keys.

### Parental controls

`core/parental/RatingPolicy.kt` enforces max-rating; PIN hashing in `PinHasher` (test exists). `features/parental/ParentalControlsScreen` and `PinEntryDialog` are reused from settings and from playlist-management "add source" gating.

## Navigation

`core/navigation/AppNavGraph.kt` is the single NavHost. Routes declared in `AppRoute.kt` as string constants + builder helpers (`AppRoute.movieDetails(id)`, `AppRoute.player(contentId, contentType, seriesId)`). The set `mainShellRoutes` defines which destinations render the persistent shell. `MainShellScaffold.kt` hosts the left expanding nav rail (electric-blue redesign — see `docs/07_REDESIGN_CONCEPT.md`).

## Design system

`core/design/theme/` (`CinemaColors`, `CinemaTypography`, `CinemaSpacing`, `CinemaShapes`, `CinemaTheme`) — deep-space black canvas, **electric-blue** accent (`#3DA9FC`), red reserved for LIVE only. Historical `Gold*` tokens are aliased to blue for backwards compat — don't reintroduce gold.

`core/design/components/` holds every shared TV composable (`FocusableCinemaCard`, `PosterCard`, `ChannelTile`, `ContentRail`, `HeroBanner`, `CatalogStateContent`, `SyncStatusBanner`, `SkeletonComponents`, `CinemaAsyncImage`, `SearchKeyboardLayouts`, etc.). This is a **10-foot remote-first** UI: every focusable element must scale + show a blue focus border (see `docs/03_DESIGN_SYSTEM_AND_SHARED_COMPONENTS.md`).

## Supabase backend (`supabase/`)

- `migrations/00{1..4}_*.sql` — schema (`profiles`, `device_activation_sessions`, `playlist_sources`, `favorites`, `watch_history`, `user_settings`, `parental_controls`). RLS is on. Apply in order.
- `functions/approve-device-activation/` — Deno Edge Function invoked via RPC `approve_device_activation` by the companion site (uses the calling user's JWT).
- `functions/exchange-activation-session/` — Edge Function the TV polls to swap an approved activation code for auth tokens.
- `activation-companion/` — static site (vanilla JS) hosted at `https://iptv.afifistudio.com`. `config.js` is git-ignored; deploy from `config.example.js`. Supports email/password + Google OAuth (PKCE).
- Flow: TV shows QR + code (`?activation=XXXX-XX`) → user signs in on web → RPC marks session `APPROVED` → TV polls `exchange-activation-session` → tokens.

## Strings, plurals, localization

`app/src/main/res/values/strings.xml` is the default; `values-ar/` is the Arabic locale. `plurals.xml` holds quantity strings (e.g. season count). `core/util/AppStrings.kt` is the indirection ViewModels use to keep strings out of pure Kotlin tests — prefer it over direct `context.getString` in VMs.

## Testing notes

19 unit-test files live under `app/src/test/java/com/iptvcinema/tv/...` mirroring `core/` and `features/`. They cover the pure logic paths: normalizers (Xtream/M3U/XMLTV), parental gate + rating policy, episode sequence + watch-history resume, EPG window math, guide layout, player key handler, session state, Supabase mappers, sync-status formatter, PIN hasher, cloud-credentials cipher. **No ViewModel/Composable/UI tests exist yet** — when adding one, follow the existing `*Test.kt` naming and prefer hand-written fakes over mocking frameworks.

## Docs pack (`docs/`)

Authoritative specs — read before non-trivial work:

- `00_README.md` — index + implementation status (Phases 1–10 done).
- `01_PRODUCT_SCOPE.md` — modules, user flows, legal boundaries.
- `02_TECH_ARCHITECTURE.md` — stack, package layout, data flow, Xtream/M3U/EPG notes.
- `03_DESIGN_SYSTEM_AND_SHARED_COMPONENTS.md` — tokens, components, focus rules.
- `04_SCREEN_PROMPTS.md` — per-screen UX briefs.
- `05_DATA_MODELS_AND_SUPABASE_SCHEMA.md` — app models + Supabase tables.
- `06_IMPLEMENTATION_ROADMAP.md` — build phases + manual TV verification checklist.
- `07_REDESIGN_CONCEPT.md` — electric-blue + left nav rail redesign.
- `docs/ui/*.png` — reference mocks for each screen.

## Working agreement

- New screen → create `features/<name>/` with `*Screen.kt` + `*ViewModel.kt`, add a route to `AppRoute.kt`, register it in `AppNavGraph.kt` inside `SessionRouteGuard`.
- New catalog-scoped data → extend `core/model/catalog/CatalogModels.kt`, the matching Room entity in `core/database/entity/CatalogEntities.kt`, and bump `IptvDatabase` version with a migration (or destructive fallback if pre-release). DAOs live in `CatalogDaos.kt`.
- New IPTV source format → add `core/<format>/` with parser + normalizer + sync repository feeding `CatalogDaoFacade`.
- Demo-mode fallbacks route through `core/data/fake/FakeDataProvider.kt` — keep it deterministic for previews/tests.
- Keep `usesCleartextTraffic="true"` in `AndroidManifest.xml` — many IPTV providers still serve HTTP streams.
