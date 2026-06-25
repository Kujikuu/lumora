# IPTV Cinema — Design System and Shared Components

## Visual Direction

IPTV Cinema uses a premium cinematic design system:

```text
Deep space-black canvas + electric blue accent + glass panels + poster-led browsing
```

> See `docs/07_REDESIGN_CONCEPT.md` for the full redesign concept (electric-blue
> system + left expanding navigation rail).

## Design Keywords

- Premium
- Cinematic
- Calm
- Elegant
- Dark
- Focused
- Remote-friendly
- Electric-blue accent (red reserved for LIVE)
- High contrast
- 10-foot readable

## Color Tokens

```kotlin
object CinemaColors {
    val Background = Color(0xFF05080B)
    val BackgroundSoft = Color(0xFF081018)
    val Surface = Color(0xFF10161C)
    val SurfaceSoft = Color(0xFF151B22)
    val SurfaceGlass = Color(0xCC121820)

    // Primary accent: electric blue.
    val Accent = Color(0xFF3DA9FC)
    val AccentSoft = Color(0xFF8FD0FF)
    val AccentDeep = Color(0xFF1E6FD9)
    val AccentGlow = Color(0xFF3DA9FC)

    // Backwards-compatible aliases: the historical Gold* tokens now map to blue.
    val Gold = Accent
    val GoldSoft = AccentSoft
    val GoldDeep = AccentDeep

    // Sparing secondary warmth only (premium / top-rated flourishes).
    val AmberWarm = Color(0xFFFFC95C)

    val TextPrimary = Color(0xFFF4F7FA)
    val TextSecondary = Color(0xFFB8C0C8)
    val TextMuted = Color(0xFF747B83)

    val Border = Color(0xFF2B3138)
    val FocusBorder = Color(0xFF6FC2FF)

    val LiveRed = Color(0xFFE02424) // LIVE badges only
    val Success = Color(0xFF45C979)
    val Warning = Color(0xFFFFB84D)
}
```

## Typography Direction

Use a strong readable sans-serif for app UI.

Optional pairing:

- UI font: modern geometric sans.
- Hero/title font: elegant serif-like display style.

For implementation, start with system fonts until branding is finalized.

TV scale:

```text
Display title: 56sp–72sp
Page title: 42sp–56sp
Section title: 24sp–30sp
Body: 18sp–22sp
Metadata: 14sp–18sp
Button: 18sp–22sp
```

## Spacing Tokens

```kotlin
object CinemaSpacing {
    val ScreenPadding = 48.dp
    val SectionGap = 32.dp
    val CardGap = 18.dp
    val RailGap = 24.dp
    val ButtonGap = 16.dp
}
```

## Shape Tokens

```kotlin
object CinemaShapes {
    val Small = RoundedCornerShape(10.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(24.dp)
    val XLarge = RoundedCornerShape(32.dp)
}
```

## Focus Rules

Remote focus must be obvious.

Focused item should:

- Scale slightly.
- Show electric-blue border.
- Increase shadow/glow (blue).
- Raise z-index visually.
- Never rely only on color.
- Keep text readable.

Recommended focus behavior:

```text
Default: 1px subtle border
Focused: 2px electric-blue border + blue glow + 1.04 scale
Pressed: 0.98 scale
Disabled: low opacity
```

## Shared Components

### 1. `CinemaScreen`

Purpose:

- Provides background.
- Applies safe padding.
- Hosts common nav if needed.

Props:

```kotlin
@Composable
fun CinemaScreen(
    showTopNav: Boolean = true,
    selectedNavItem: NavItem? = null,
    content: @Composable () -> Unit
)
```

Used by:

- Home
- Live TV
- Movies
- Series
- Search
- My List
- Settings

---

### 2. `CinemaNavRail`

Purpose:

- Left vertical navigation rail for main app screens (replaces the old top nav).

Items (top to bottom):

```text
Profile (avatar)
Home
Live TV
Movies
Series
Favorites
Search
Settings (pinned at the bottom)
```

Props:

```kotlin
@Composable
fun CinemaNavRail(
    selected: NavItem,
    onNavigate: (NavItem) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
)
```

Behavior:

- Collapsed (~88dp): icons only.
- Focused: expands (~248dp) and reveals labels, overlaying content with a scrim
  so nothing reflows.
- D-pad up/down moves within the rail; right enters content; left at the content
  edge returns to the rail.
- Selected item: blue pill + blue tint. Focused item: scale + blue glow.

---

### 3. `FocusableCinemaCard`

Purpose:

Base card wrapper for remote focus.

```kotlin
@Composable
fun FocusableCinemaCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.(focused: Boolean) -> Unit
)
```

Use for:

- Posters
- Channel tiles
- Profile cards
- Settings rows
- Program cells
- Language chips

---

### 4. `CinemaButton`

Variants:

```text
PrimaryGold
SecondaryDark
Danger
Ghost
Icon
```

Props:

```kotlin
@Composable
fun CinemaButton(
    text: String,
    icon: ImageVector? = null,
    variant: CinemaButtonVariant,
    onClick: () -> Unit
)
```

Use for:

- Watch Now
- Details
- Trailer
- Record
- Add to Favorites
- Sign In
- Create Account

---

### 5. `HeroBanner`

Purpose:

Large cinematic featured content area.

Props:

```kotlin
@Composable
fun HeroBanner(
    title: String,
    subtitle: String?,
    metadata: List<String>,
    description: String,
    backdropUrl: String?,
    badges: List<Badge>,
    primaryAction: HeroAction,
    secondaryAction: HeroAction?,
    onClick: () -> Unit
)
```

Used by:

- Home
- Movies
- Series
- Details variant

---

### 6. `ContentRail`

Purpose:

Horizontal row of cards.

Props:

```kotlin
@Composable
fun <T> ContentRail(
    title: String,
    items: List<T>,
    itemContent: @Composable (T) -> Unit
)
```

Used by:

- Continue Watching
- Trending Now
- Arabic Picks
- New Releases
- More Like This
- Recently Watched

---

### 7. `PosterCard`

Purpose:

Poster tile for movies/series.

Fields:

```text
Poster image
Title
Year
Runtime / episode count
4K badge
Progress bar
Favorite/bookmark
```

Variants:

```text
PortraitPoster
LandscapePoster
CompactPoster
```

---

### 8. `ChannelTile`

Purpose:

Live TV channel tile.

Fields:

```text
Channel logo
Channel name
LIVE badge
HD/4K badge
Current program
Progress bar
```

---

### 9. `ProgramGuideGrid`

Purpose:

Electronic Program Guide.

Must support:

- Channel column.
- Timeline header.
- Current time indicator.
- Program cells.
- Focused program.
- Horizontal and vertical movement.

Performance rules:

```text
Render visible window only.
Limit visible time range.
Use lazy rows/columns.
Avoid measuring all program cells.
```

---

### 10. `SearchKeyboard`

Purpose:

TV-friendly on-screen keyboard.

Features:

- D-pad navigation.
- Backspace.
- Clear.
- Space.
- Voice search button.
- Recent searches.
- Results refresh.

---

### 11. `PlayerOverlay`

Purpose:

Custom Media3 overlay.

Includes:

- Title
- Metadata
- Play/pause
- Seek forward/back
- Progress bar
- Chapter markers
- Subtitles
- Audio
- Quality
- Up Next

---

### 12. `SettingsPanel`

Purpose:

Two-column TV settings layout.

Includes:

- Left menu.
- Right detail panel.
- Toggle rows.
- Selectable rows.
- Account summary.

## Component Consistency Rules

Every UI prompt and screen must use:

```text
Brand name: IPTV Cinema
Background: dark blue-black / graphite
Accent: electric blue (red reserved for LIVE)
Cards: rounded glass panels
Focus: electric-blue outline + glow
Typography: large readable TV typography
Navigation: left expanding rail for main app screens
No phone layout
No mobile bottom tabs
No clutter
No real copyrighted brand names
No real channel logos unless licensed
```

## Reusable Screen Layouts

### Layout A — Centered Launcher

Used by:

- Splash
- Profile Selection

```text
Logo
Large centered title/content
Minimal controls
Gold focus state
```

### Layout B — Split Utility

Used by:

- Activation
- Add Source
- Parental PIN

```text
Left: form/instructions
Right: visual panel/QR/status
```

### Layout C — Browse

Used by:

- Home
- Movies
- Series
- My List

```text
Top nav
Page title or hero
Filter chips
Rails/grid
Footer hints
```

### Layout D — EPG

Used by:

- Live TV
- TV Guide

```text
Top nav
Preview panel
On Now panel
Category chips
Timeline grid
Remote hints
```

### Layout E — Detail

Used by:

- Movie Details
- Series Details
- Channel Details

```text
Hero backdrop
Title metadata actions
Cast/languages
More like this
```

### Layout F — Player

Used by:

- VOD player
- Live player

```text
Fullscreen video
Temporary overlay
Controls
Timeline or live indicator
Audio/subtitle/quality
```
