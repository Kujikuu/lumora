# IPTV Cinema — Product Scope

## Product Summary

**IPTV Cinema** is a premium Android TV IPTV app for watching live TV, movies, series, and playlist-based media through supported IPTV sources.

The app supports:

- Xtream Codes accounts.
- M3U playlists.
- EPG/XMLTV program guide.
- Supabase authentication.
- Multi-profile viewing.
- Favorites / My List.
- Continue Watching.
- Playback settings.
- Parental controls.
- Device activation.

## Positioning

IPTV Cinema should feel like a premium cinematic TV experience, not a generic IPTV utility.

The design direction:

- Dark cinema interface.
- Gold/champagne highlights.
- Premium poster-led browsing.
- Smooth remote control navigation.
- Strong Live TV and EPG experience.
- Clear separation between user-provided IPTV sources and app-owned UI/metadata.

## Important Legal Boundary

The app should be positioned as a **player and playlist manager**.

Do not ship illegal channels or copyrighted streams inside the app.

Safe positioning:

```text
IPTV Cinema lets users connect their own IPTV subscriptions or playlists.
The app does not provide or sell copyrighted channels, movies, or series.
```

Avoid:

```text
Free movies
Free sports
Free premium channels
Netflix replacement
Pirated IPTV bundle
```

## Supported Source Types

### 1. Xtream Codes

User enters:

- Server URL
- Username
- Password
- Optional account name

App retrieves:

- Live categories
- Live streams
- VOD categories
- VOD streams
- Series categories
- Series streams
- EPG data when available

### 2. M3U Playlist

User enters:

- M3U URL
- Optional EPG URL
- Optional playlist name
- Optional user-agent/header settings

App parses:

- Channel name
- Logo
- Group/category
- TVG ID
- Stream URL
- Catch-up attributes if available

### 3. Manual Playlist Upload

Optional later phase:

- User uploads `.m3u` file through mobile/web companion.
- Supabase stores playlist metadata.
- Actual stream URLs may be encrypted or stored locally depending on privacy decision.

## User Roles

### Viewer

Can:

- Add playlist/source.
- Watch live TV.
- Browse movies/series.
- Save favorites.
- Manage profiles.
- Configure player preferences.

### Admin / Owner

Possible later phase.

Can:

- Manage app announcements.
- Manage curated metadata.
- Monitor usage analytics.
- Manage support tickets.
- Configure public app settings.

## Key User Flows

### First Launch Flow

```text
Splash
→ Activate TV
→ Sign in or create account
→ Add IPTV source
→ Choose profile
→ Home
```

### Returning User Flow

```text
Splash
→ Choose profile
→ Home
```

### Playback Flow

```text
Home / Live TV / Movies / Series / Search
→ Details or Channel Preview
→ Player
→ Resume / Favorite / Up Next
```

### Add Xtream Codes Flow

```text
Settings / Playlist Management
→ Add Source
→ Xtream Codes
→ Enter server, username, password
→ Validate connection
→ Sync categories and streams
→ Home
```

### Add M3U Flow

```text
Settings / Playlist Management
→ Add Source
→ M3U Playlist
→ Enter playlist URL
→ Optional EPG URL
→ Validate playlist
→ Parse channels
→ Home
```

## Main Product Modules

| Module | Purpose |
|---|---|
| Auth | Supabase login/session/device activation. |
| Profiles | Main, Family, Kids, Guest, parental restrictions. |
| Source Manager | Xtream Codes and M3U playlist accounts. |
| Home Aggregator | Mixes continue watching, live channels, VOD, series, recommendations. |
| Live TV | Channel playback and quick access. |
| EPG | Program guide from XMLTV or provider API. |
| VOD | Movies from Xtream/M3U/custom metadata. |
| Series | Series and episodes. |
| Player | Media3 playback. |
| Search | Unified search across channels, movies, series, categories. |
| My List | Saved content and channels. |
| Settings | Playback, account, language, parental controls. |

## MVP Scope

Build the MVP in this order:

1. Native Android TV shell.
2. Supabase auth.
3. Profile selection.
4. Add one Xtream Codes source.
5. Sync live categories and channels.
6. Home screen with synced Xtream catalog (browse rails); Details/Search/Player remain prototype until later phases.
7. Live TV screen.
8. Media3 player.
9. Favorites.
10. Settings.

Phase 6 satisfies steps 4–5 and partial step 6–7 (browse only). Full MVP requires Phase 8 (Player) + favorites wiring on real content IDs. See `06_IMPLEMENTATION_ROADMAP.md` MVP milestone.

M3U should be second after Xtream Codes unless the business priority is playlist import first.
