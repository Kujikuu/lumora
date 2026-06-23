# IPTV Cinema — Consistent UI Screen Prompts

## Data source note (Phase 6 wiring status)

```text
Real Room catalog: Home, Live TV, Movies, Series (non-demo mode)
Still fake/prototype: Details, Search, Player overlay
Prompts below describe target UX; data source may be fake until later phases.
```

## Global Prompt Prefix

Use this prefix before every screen prompt to keep the UI consistent:

```text
Create a single 16:9 Android TV UI mockup screen for a native Kotlin app called "IPTV Cinema".

Keep the same cohesive design system across all screens:
- Premium dark cinema interface.
- Deep blue-black / graphite background.
- Champagne gold / amber accent color.
- Elegant rounded cards and glass-like panels.
- Strong gold focus outline for remote-control navigation.
- Large readable typography for 10-foot TV viewing.
- Polished Android TV layout, not a mobile layout.
- Top navigation on main app screens.
- Fictional content only.
- No real streaming brand logos.
- No copyrighted channel logos.
- No device frame, no room scene, no hands, no extra presentation objects.
- The image must contain only the full TV app UI screen itself.
```

---

# 1. Splash / Launcher Screen Prompt

```text
Create the splash / launcher screen before the home screen.

Show the IPTV Cinema logo large and centered with a refined gold play-mark icon.
Use a cinematic abstract background with subtle golden light streaks, soft smoky texture, and a luxury dark ambience.
Add a short tagline: "Your premium entertainment hub".
Add a subtle loading indicator near the bottom with text: "Preparing your experience".
Keep the screen minimal, elegant, and premium.
No content posters.
No navigation.
No buttons.
```

## Screen Purpose

First impression. Brand confidence.

## Shared Components

- `CinemaScreen`
- `CinemaLogo`
- `LoadingIndicator`

---

# 2. Device Activation / Sign In Prompt

```text
Create the device activation / sign-in screen for Android TV.

Left side:
- IPTV Cinema logo.
- Large heading: "Activate Your TV".
- Instructions:
  1. On your phone or computer, go to iptvcinema.tv/link
  2. Enter the activation code shown below.
- Large activation code: C7K4-9P.
- Primary gold button: "Enter Existing Account".
- Secondary dark button: "Create Account".

Right side:
- Large QR-code style panel with a gold border.
- Caption: "Scan to sign in".
- Supporting text: "Use your phone camera to quickly open the activation page."

Bottom:
- Benefit row with icons:
  Sync Watchlist
  Resume Anywhere
  Personalized Recommendations

Use the same dark graphite and champagne gold design system.
```

## Screen Purpose

TV login without typing long credentials.

## Shared Components

- `CinemaLogo`
- `CinemaButton`
- `QrActivationPanel`
- `BenefitItem`
- `FocusableCinemaCard`

---

# 3. Add Source Choice Prompt

```text
Create the "Add IPTV Source" screen.

Show IPTV Cinema logo at top left.
Main heading: "Add Your IPTV Source".
Subtitle: "Connect your provider to start watching live TV, movies, and series."

Show three large selectable cards:
1. Xtream Codes
   Description: "Use server URL, username, and password."
   Icon: secure server / key.
2. M3U Playlist
   Description: "Add a playlist URL with optional EPG."
   Icon: playlist / link.
3. Demo Mode
   Description: "Preview the app with sample content."
   Icon: play / sparkle.

Highlight the focused card with a gold glow and rounded border.
Add a bottom note:
"IPTV Cinema does not provide channels or streams. Use your own legal provider."
```

## Screen Purpose

Choose provider connection type.

## Shared Components

- `SourceTypeCard`
- `CinemaButton`
- `FooterNote`

---

# 4. Xtream Codes Login Prompt

```text
Create the Xtream Codes connection screen.

Left side:
- Heading: "Connect Xtream Codes".
- Description: "Enter your IPTV provider details."
- Three large TV-friendly input fields:
  Server URL
  Username
  Password
- Toggle: "Remember this source".
- Primary gold button: "Connect".
- Secondary button: "Back".

Right side:
- Glass panel explaining:
  "Your credentials are used only to connect to your IPTV provider."
  "You can remove this source anytime."
- Status preview area for connection validation:
  Server Reachable
  Account Active
  Channels Found
  EPG Available

Use the IPTV Cinema dark/gold visual system.
Make the focused field obvious with a gold border.
```

## Screen Purpose

Connect Xtream provider.

## Shared Components

- `CinemaTextField`
- `CinemaButton`
- `ValidationStatusItem`
- `FocusableCinemaCard`

---

# 5. M3U Playlist Login Prompt

```text
Create the M3U Playlist connection screen.

Left side:
- Heading: "Add M3U Playlist".
- Description: "Paste your playlist URL and optional EPG guide."
- Input fields:
  Playlist Name
  M3U URL
  EPG URL Optional
- Advanced collapsed section:
  User-Agent
  Referer
  Headers
- Primary gold button: "Import Playlist".
- Secondary button: "Back".

Right side:
- Preview panel showing:
  Playlist validation
  Channels found
  Groups detected
  EPG match rate

Use dark cinema background, gold focus states, and rounded glass panels.
```

## Screen Purpose

Import M3U playlist.

## Shared Components

- `CinemaTextField`
- `AdvancedOptionsCard`
- `ImportPreviewPanel`
- `CinemaButton`

---

# 6. Profile Selection Prompt

```text
Create the "Choose Profile" screen.

Top left:
- IPTV Cinema logo.

Center:
- Large title: "Choose Profile".
- Subtitle: "Who's watching?"

Show four large selectable profile cards:
1. Main
2. Family
3. Kids
4. Guest

Each card has a circular cinematic avatar.
Highlight "Main" with a gold focus glow.
Add a centered "Manage Profiles" button below.

Keep the layout uncluttered and premium.
```

## Screen Purpose

Select viewing profile.

## Shared Components

- `ProfileCard`
- `CinemaButton`
- `FocusableCinemaCard`

---

# 7. Home Screen Prompt

```text
Create the IPTV Cinema Home screen.

Top:
- IPTV Cinema logo.
- Navigation: Home, Live TV, Movies, Series, Sports, Kids, My List, Search.
- Home selected with a gold pill.

Hero:
- Large cinematic featured banner.
- Fictional title: "Shadows of Aurora".
- Metadata: 2024, Action Adventure, 1h 57m, 4K, Dolby Audio.
- Description.
- Primary button: Watch Now.
- Secondary button: Details.
- Carousel dots.

Below:
- Continue Watching rail with progress bars.
- Trending Now poster rail.
- Arabic Picks rail with Arabic fictional titles.
- Live Channels tile rail with LIVE badges.
- New Releases rail.

Show one focused card with gold outline.
```

## Screen Purpose

Main content discovery.

## Shared Components

- `CinemaTopNav`
- `HeroBanner`
- `ContentRail`
- `PosterCard`
- `LandscapeContentCard`
- `ChannelTile`

---

# 8. Live TV Screen Prompt

```text
Create the Live TV screen.

Top:
- IPTV Cinema logo.
- Main navigation.
- Live TV selected.

Upper left:
- Large current channel preview card.
- LIVE badge.
- Channel: Nature World HD.
- Program: Wild Kingdoms.
- Time: 10:00 AM – 11:00 AM.
- Progress bar.
- Buttons: Watch Fullscreen, Record.

Upper right:
- "On Now" compact panel listing current programs across channels.

Middle:
- Category chips:
  All Channels, News, Sports, Movies, Kids, Entertainment, Documentary, Lifestyle.

Bottom:
- Electronic Program Guide grid.
- Left column: channel logos/names.
- Top row: timeline.
- Current time vertical indicator.
- Focused program cell with gold outline.

Footer:
- Remote hints: OK Select, Record, +24h, -24h, Filter.
```

## Screen Purpose

Live viewing and EPG browsing.

## Shared Components

- `LivePreviewCard`
- `OnNowPanel`
- `CategoryChip`
- `ProgramGuideGrid`
- `RemoteHintBar`

---

# 9. Movies Screen Prompt

```text
Create the Movies browse screen.

Top:
- IPTV Cinema logo.
- Main navigation.
- Movies selected.

Page title: "Movies".

Featured strip:
- Fictional title: "Beyond the Horizon".
- Metadata: 2024, Sci-Fi Adventure, 2h 15m, 4K, Dolby Atmos.
- Buttons: Watch Now, Details.

Filters:
- All, Action, Drama, Arabic, Family, 4K, New, Top Rated.
- Sort: Newest.
- Filter button.

Main:
- Poster grid with cinematic fictional titles.
- Include English and Arabic posters.
- One focused poster with gold glowing border.
- 4K badges on selected items.

Keep layout spacious and remote-friendly.
```

## Screen Purpose

Browse VOD movies.

## Shared Components

- `CinemaTopNav`
- `FeaturedStrip`
- `FilterChipRow`
- `PosterGrid`
- `SortButton`
- `FilterButton`

---

# 10. Series Screen Prompt

```text
Create the Series browse screen.

Top:
- IPTV Cinema logo.
- Main navigation.
- Series selected.

Page title: "Series".

Featured strip:
- Fictional series title: "The Last Kingdoms".
- Metadata: 2024, Fantasy Drama, 2 Seasons, 4K.
- Buttons: Watch Now, Details.

Filters:
- All, Drama, Action, Arabic, Kids, New Episodes, 4K, Top Rated.

Main:
- Series poster grid.
- Cards include season count or latest episode badge.
- Include a "New Episode" badge on some cards.
- One focused poster with gold focus outline.

Bottom:
- Continue Series rail.
```

## Screen Purpose

Browse series catalog.

## Shared Components

- `FeaturedStrip`
- `SeriesPosterCard`
- `FilterChipRow`
- `PosterGrid`

---

# 11. Details Screen Prompt

```text
Create a Movie Details screen.

Top:
- IPTV Cinema logo.
- Main navigation.

Hero:
- Large cinematic backdrop.
- Fictional title: "Dragon's Oath".
- Metadata: 2024, Fantasy Adventure, 2h 18m, 4K, rating 8.7.
- Short synopsis.
- Buttons: Watch Now, Trailer, Add to Favorites.

Below:
- Cast cards row.
- Available Languages chips:
  English, Arabic, Hindi, Spanish, French.
- Subtitle chips:
  English, Arabic, Hindi, Spanish, French, Turkish.

Bottom:
- More Like This rail.

Use premium dark/gold style and strong TV focus state.
```

## Screen Purpose

Decision screen before playback.

## Shared Components

- `DetailHero`
- `CinemaButton`
- `CastCard`
- `LanguageChip`
- `ContentRail`

---

# 12. Series Details Prompt

```text
Create a Series Details screen.

Hero:
- Fictional title: "Kingdom Fallen".
- Metadata: 2024, Drama Fantasy, 3 Seasons, 4K, rating 8.5.
- Description.
- Buttons: Resume, Trailer, Add to Favorites.

Below:
- Season selector pills: Season 1, Season 2, Season 3.
- Episode rail/grid:
  Episode thumbnail
  Episode number
  Title
  Duration
  Progress if partially watched

Right or lower area:
- Cast row.
- Languages/subtitles.
- More Like This.

Make the selected episode focused with a gold outline.
```

## Screen Purpose

Series detail + episode selection.

## Shared Components

- `DetailHero`
- `SeasonSelector`
- `EpisodeCard`
- `CastCard`

---

# 13. Player Screen Prompt

```text
Create the video Player screen.

Background:
- Full-screen cinematic video still.

Overlay:
- Top left:
  IPTV Cinema logo.
  "Now Playing".
  Title: "Shadows of Aurora".
  Metadata: 2024, Action Adventure, 1h 57m, 4K, Dolby Audio.

Center:
- Playback controls:
  Rewind
  Rewind 10
  Play/Pause large gold center button
  Forward 10
  Next

Bottom:
- Progress timeline.
- Elapsed time and remaining time.
- Chapter markers.
- Subtitle button.
- Audio button.
- Quality badge 4K.
- Up Next rail with related content.

Use elegant translucent panels.
Keep controls readable and not cluttered.
```

## Screen Purpose

Playback experience.

## Shared Components

- `PlayerOverlay`
- `PlaybackControlButton`
- `ProgressTimeline`
- `ChapterMarker`
- `UpNextRail`

---

# 14. Search Screen Prompt

```text
Create the Search screen.

Top:
- IPTV Cinema logo.
- Main navigation.
- Search selected.

Left:
- Page title: Search.
- Subtitle: "Find your favorite movies, series, and live channels."
- Large focused search field with typed query: "shadow".
- Voice search button.
- TV on-screen keyboard.

Right:
- Recent Searches chips:
  shadow, eclipse, ocean, kingdom, lost city.
- Results for "shadow".
- Filter chips:
  All, Movies, Series, Live TV, Channels.
- Top Results poster row.
- Series Results row.
- Live Channels row.

Highlight one result with gold focus outline.
```

## Screen Purpose

Unified search.

## Shared Components

- `SearchInput`
- `SearchKeyboard`
- `RecentSearchChip`
- `SearchResultsSection`
- `PosterCard`
- `ChannelTile`

---

# 15. My List Prompt

```text
Create the My List / Favorites screen.

Top:
- IPTV Cinema logo.
- Main navigation.
- My List selected.

Page:
- Title: "My List".
- Subtitle: "All your saved movies, series, and channels in one place."
- Saved count: 42 saved items.

Filters:
- All, Movies, Series, Channels, Continue Watching.
- Sort: Recently Added.
- Filter button.

Main:
- Grid of saved posters and live channel cards.
- Cards show bookmark icon.
- Some cards show progress percentage.
- One focused card with gold border.

Below:
- Recently Watched rail.

Footer:
- Tip: Long press on any item to remove it from your list.
```

## Screen Purpose

Saved items and quick resume.

## Shared Components

- `SavedItemCard`
- `PosterGrid`
- `FilterChipRow`
- `ContentRail`

---

# 16. Settings Prompt

```text
Create the Settings screen.

Top:
- IPTV Cinema logo.
- Main navigation.
- Settings icon selected.

Left:
- Page title: Settings.
- Subtitle: "Manage your account, preferences, and device settings."
- Vertical menu:
  Account
  Subscription
  Playback
  Language
  Parental Controls
  Notifications
  Device Preferences
  About

Highlight Playback with a gold focus state.

Right:
- Account summary card:
  Avatar
  Name
  Email
  Premium Plan
  Renewal date
  Manage Account button.

Playback settings list:
- Streaming Quality: Auto Best Available.
- Default Audio: English.
- Subtitles: Off.
- Autoplay Next Episode: On.
- Continue Watching: On.
- Skip Intro: Off.
- Theme: Dark Cinema.
- Data Saver: Off.

Use TV-friendly toggles, chevrons, and spacing.
```

## Screen Purpose

Preferences and account management.

## Shared Components

- `SettingsMenu`
- `SettingsRow`
- `SettingsToggle`
- `AccountSummaryCard`

---

# 17. Playlist Management Prompt

```text
Create the Playlist Management screen.

Top:
- IPTV Cinema logo.
- Main navigation.
- Settings or Sources selected.

Page title:
"Playlist Sources"

Subtitle:
"Manage your Xtream Codes accounts and M3U playlists."

Main:
- Cards for connected sources:
  1. Family IPTV — Xtream Codes — Active — 1,240 channels — Last synced 12 min ago.
  2. Sports Pack — M3U Playlist — Needs attention — EPG missing.
  3. Kids Playlist — M3U Playlist — Active.

Each card includes:
- Source type badge.
- Status.
- Sync button.
- Edit button.
- Remove button.

Right side or top:
- Gold button: Add Source.

Bottom:
- Sync status panel:
  Channels
  Movies
  Series
  EPG
  Last update

Highlight one source card with gold focus outline.
```

## Screen Purpose

Manage IPTV providers/playlists.

## Shared Components

- `SourceCard`
- `StatusBadge`
- `CinemaButton`
- `SyncStatusPanel`

---

# 18. Parental Controls Prompt

```text
Create the Parental Controls screen.

Top:
- IPTV Cinema logo.
- Main navigation.

Left:
- Settings vertical menu.
- Parental Controls selected.

Right:
- Heading: "Parental Controls".
- Profile selector chips: Main, Family, Kids, Guest.
- PIN status: Enabled.
- Button: Change PIN.
- Rating restrictions:
  G, PG, 12+, 16+, 18+.
- Content type restrictions:
  Hide Adult Categories
  Lock Purchases
  Lock Playlist Settings
  Require PIN for Live TV categories.
- Blocked categories list.

Use gold focus states and clear toggles.
```

## Screen Purpose

Protect family/kids profiles.

## Shared Components

- `ProfileChip`
- `SettingsToggle`
- `RatingRestrictionSelector`
- `BlockedCategoryList`

---

# 19. Empty State Prompt

```text
Create an empty state screen for IPTV Cinema.

Scenario:
No IPTV source connected.

Show:
- IPTV Cinema logo.
- Large friendly title: "No source connected".
- Description: "Add your Xtream Codes account or M3U playlist to start watching."
- Primary gold button: "Add Source".
- Secondary button: "Try Demo Mode".
- Small legal note: "IPTV Cinema does not provide channels or streams."

Use the same premium dark/gold style.
```

## Screen Purpose

Recover from missing data.

## Shared Components

- `EmptyState`
- `CinemaButton`

---

# 20. Error State Prompt

```text
Create an error state screen for IPTV Cinema.

Scenario:
Stream failed to play.

Show:
- Dark player background.
- Warning icon in gold.
- Title: "Stream unavailable".
- Description: "This channel could not be loaded. Try again or choose another stream."
- Buttons:
  Try Again
  Switch Stream
  Back to Guide
- Small technical line:
  Error code: STREAM_TIMEOUT

Keep the UI calm, premium, and practical.
```

## Screen Purpose

Recover from bad IPTV streams.

## Shared Components

- `ErrorState`
- `CinemaButton`
