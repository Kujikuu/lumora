# IPTV Cinema Focused Premium V1 Design

Date: 2026-07-02

## Product Boundary

IPTV Cinema V1 is a premium, account-required native Android TV IPTV player for users who already have legal Xtream Codes subscriptions. It is not an IPTV provider, reseller panel, or content catalog owner.

V1 includes:

- TV activation through a phone/web companion.
- Passphrase-based encrypted cloud sync for Xtream credentials.
- Multiple adult profiles.
- Live TV, Movies, and Series.
- Now/Next EPG.
- Unified search across the synced Xtream catalog.
- Favorites, continue watching, and playback progress sync.
- Arabic-first bilingual UI.
- Google Play-compatible legal positioning.
- Android TV 9 reliability and D-pad navigation.

V1 excludes:

- M3U.
- Parental controls.
- Play Billing.
- Admin console.
- White-label provider mode.
- Full EPG grid.
- Catch-up TV.
- External player handoff.
- Bundled streams.

## Architecture

Keep the existing native Android TV stack: Kotlin, Jetpack Compose for TV, Media3 ExoPlayer, Hilt, Room, DataStore, Coil, Retrofit/OkHttp, and Supabase Auth/Postgres/Edge Functions.

Responsibilities:

- TV app: 10-foot UI, D-pad focus, local playback, local catalog cache, profile switching, favorites/progress UI, and offline-tolerant browsing.
- Companion web app: account sign-in, activation approval, encryption passphrase setup, and Xtream credential entry.
- Supabase: auth, activation sessions, profiles, devices, encrypted source blobs, favorites, watch history, and playback progress.
- Room on TV: large Xtream catalog cache: channels, movies, series, episodes, categories, and EPG rows.

The important rule: encrypted source credentials sync through Supabase, but full IPTV catalogs stay local on each TV. Supabase should not store provider catalogs in V1.

## Core User Flows

### First TV Setup

Splash -> Activation screen -> scan QR/open companion -> sign in/create account -> enter encryption passphrase -> add Xtream Codes source on companion -> TV receives approved session -> TV downloads encrypted source blob -> user enters/unlocks passphrase on TV -> sync Xtream catalog -> create/select profile -> Home.

### Returning User

Splash -> session check -> profile selection -> Home.

If the source is encrypted but locked, show a focused unlock screen before catalog sync or playback.

### New Device

Activation -> sign in on companion -> approve TV -> TV downloads encrypted source blob -> user enters passphrase -> local catalog sync -> profile selection.

### Daily Viewing

Home/Live TV/Movies/Series/Search/My List -> details or channel preview -> player -> progress/favorite/watch history syncs back to Supabase.

### Source Failure

If Xtream auth or sync fails, keep the last good local catalog where possible, show clear source health, and offer retry/reconnect through the companion. Do not run destructive resync unless the user removes the source.

## UI And Navigation

IPTV Cinema V1 uses a premium dark cinematic identity with electric-blue focus, red only for LIVE, strong poster/channel artwork, and no cloned Netflix/Shahid layouts or colors. Shahid and Netflix are references for quality, pacing, and confidence, not assets or exact screen structure.

Navigation is Android TV first:

- Left expanding nav rail: Profile, Home, Live TV, Movies, Series, My List, Search, Settings.
- D-pad behavior is predictable: Up/Down changes rows or rail items, Left/Right moves inside rails and between zones, Back exits overlays before screens.
- Every focusable item has scale, border, glow, and readable label state. Focus cannot disappear.
- Animations are lightweight and Android TV 9 safe: focus scale, fades, subtle panel movement, no heavy blur or shader effects.

Core screens:

- Activation: code/QR, companion instructions, account/security messaging.
- Unlock Source: passphrase entry, source name, privacy explanation.
- Profile Selection: multiple adult profiles, simple add/rename/delete.
- Home: hero, continue watching, live now, popular movies, trending series, favorites.
- Live TV: categories, preview/action area, channel list with Now/Next.
- Movies/Series: category filters, poster rails/grids, details pages.
- Player: clean overlay, live/channel controls, VOD seek controls, retry/error states.
- Settings: account, devices, source status, language, playback, legal/about.

## Data And Security

V1 uses zero-knowledge-style source sync.

Xtream credentials are encrypted before upload using a user passphrase. Supabase stores only encrypted source blobs plus safe metadata: source name, type, created date, last sync status, and linked devices. The backend should not be able to read server URL, username, or password.

On each TV:

- User activates the device through the companion.
- TV downloads the encrypted source blob.
- User enters the passphrase to decrypt locally.
- Decrypted credentials stay only in encrypted local storage.
- Room stores the synced Xtream catalog locally.
- Favorites, continue watching, and playback progress sync to Supabase by account/profile/content IDs.

Recovery rule: if the user forgets the passphrase, IPTV credentials cannot be recovered. They can reset the encrypted source and add Xtream credentials again.

Removal rule: removing a source deletes the encrypted blob from Supabase and purges local credentials/catalog cache from each device on next sync.

Security basics:

- HTTPS only for app/backend communication.
- No credential logging.
- Clear device/session revocation.
- Rate-limited activation.
- Short-lived activation codes.
- Legal copy states that IPTV Cinema does not provide streams.

## Playback And Reliability

V1 player is Media3-first and optimized for real Android TV 9 devices.

Required player behavior:

- Live TV: play/pause where stream supports it, channel up/down, return-to-live, LIVE badge, current program, next program, loading and retry states.
- Movies/Series: play/pause, seek, resume, restart, next episode, previous episode where available, save progress.
- Tracks: expose subtitles/audio track selection if Media3 reports available tracks; no custom subtitle style editor in V1.
- Errors: readable messages for provider offline, auth expired, unsupported format, network timeout, and retry failed.

Reliability targets:

- Fast startup with splash resolving account/source/profile state.
- Last good catalog remains browsable if source refresh fails.
- Catalog sync is foreground and explicit in V1, with safe retry.
- Playback retries transient network failures without trapping the user.
- Focus restoration works after back navigation, player exit, and category changes.
- Heavy animation, full-screen blur, and expensive image effects are skipped for Android TV 9.

Testing acceptance:

- Unit tests for source encryption/decryption helpers, sync state rules, player key handling, resume policy, and EPG Now/Next logic.
- Manual TV checklist on Android TV 9: activation, passphrase unlock, catalog sync, D-pad browsing, live playback, VOD resume, profile switch, language switch, source failure, and logout.

## Release Scope

V1 must ship:

- Account-required Android TV app.
- Companion activation.
- Passphrase-based encrypted Xtream credential sync.
- Multiple adult profiles.
- Local Xtream catalog sync.
- Live TV, Movies, and Series.
- Now/Next EPG.
- Unified search across the synced Xtream catalog.
- Favorites, continue watching, and playback progress sync.
- Arabic-first bilingual UI.
- Production player controls.
- Google Play-compatible legal positioning.
- Android TV 9 verification.

V1 must not ship:

- M3U.
- Parental controls.
- Billing.
- Admin console.
- Provider/white-label mode.
- Full EPG grid.
- Catch-up TV.
- External player handoff.
- Catalog storage in Supabase.
- Bundled channels.
- Heavy cinematic effects that harm low-end TV hardware.

## Post-V1 Roadmap

1. M3U support with optional EPG.
2. Parental controls and Kids profile.
3. Google Play Billing or private paid access.
4. Full EPG grid.
5. Admin/support console.
6. Optional encrypted multi-source management improvements.
7. Provider/white-label mode only if the business needs it.
