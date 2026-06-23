# IPTV Cinema — Kotlin Native Android TV App Document Pack

## Purpose

This document pack converts the generated **IPTV Cinema** UI direction into a practical native Android TV implementation plan.

The app is a premium Android TV IPTV player with:

- Kotlin native Android app.
- Jetpack Compose for TV UI.
- Media3 ExoPlayer playback.
- Supabase backend for authentication, user data, profiles, favorites, watch history, settings, and playlist metadata.
- Xtream Codes support.
- M3U playlist support.
- EPG/XMLTV support.
- Premium dark cinema UI system with gold accents.

## Document Index

| File | Purpose |
|---|---|
| `01_PRODUCT_SCOPE.md` | Product scope, app modules, user flows, legal/compliance boundaries. |
| `02_TECH_ARCHITECTURE.md` | Kotlin architecture, Supabase backend, IPTV data layer, playback layer. |
| `03_DESIGN_SYSTEM_AND_SHARED_COMPONENTS.md` | UI tokens, shared components, focus behavior, reusable Compose components. |
| `04_SCREEN_PROMPTS.md` | Consistent prompt for every major IPTV Cinema UI screen. |
| `05_DATA_MODELS_AND_SUPABASE_SCHEMA.md` | App models, Supabase tables, local cache, data relationships. |
| `06_IMPLEMENTATION_ROADMAP.md` | Build order, milestones, MVP, testing checklist. |

## Implementation Status

Phases 4–10 complete. Phase 10 (Polish) finalized with production UX states, Room search, parental PIN + max-rating enforcement, sync banner, now-playing highlight, source-delete Room purge, and image fallbacks.

See `06_IMPLEMENTATION_ROADMAP.md` for per-phase deliverables, manual TV verification checklist, and scope boundaries.

Phase 6: Xtream connect + Room catalog sync + browse on Home/Live TV/Movies/Series.
Phase 8: Media3 player with live/VOD playback, resume, series continuity, and channel zapping.
Phase 9: Background EPG sync, lazy windowed TV guide, ±24h navigation, focused program preview, and current-time indicator.
Phase 10: CatalogStateContent on all browse tabs + Search, RatingPolicy, PlaybackSessionTracker, SyncStatusBanner, purgeSource on delete.

Search uses real Room catalog queries with debounced SearchViewModel and full source-health state routing.

## Core App Screens

1. Splash / Launcher
2. Activate TV
3. Choose Profile
4. Home
5. Live TV
6. TV Guide / EPG
7. Movies
8. Series
9. Details
10. Player
11. Search
12. My List
13. Settings
14. Playlist Management
15. Add Xtream Codes Account
16. Add M3U Playlist
17. Parental Controls

## Core UX Rule

This is not a mobile UI scaled up.

It is a **10-foot TV interface**.

Every screen must prioritize:

- Remote control navigation.
- Clear focus states.
- Large readable typography.
- Strong spacing.
- Low cognitive load.
- Fast access to playback.
- Reliable recovery from bad streams.
