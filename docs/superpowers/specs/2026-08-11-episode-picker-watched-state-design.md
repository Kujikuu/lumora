# Episode Picker Watched-State Design

## Goal

Make watched and partially watched episodes immediately recognizable in the player's **Choose episode** sidebar without weakening TV focus visibility or confusing completion with the currently playing state.

## Watched-State Rules

- An episode is **watched** when valid saved progress is at least 95% of its duration.
- An episode is **partially watched** when it has positive valid progress below 95%.
- An episode is **unwatched** when it has no valid saved progress.
- The currently playing episode remains a separate state. Its existing red playing treatment takes precedence for the row background, while its completion marker may still remain visible on the thumbnail.
- Invalid or missing durations do not produce a watched or progress state.

## Visual Treatment

- Watched episodes show a green circular check marker over the episode thumbnail.
- Watched thumbnails use slightly reduced opacity so the check is readable without making the row look disabled.
- Partially watched episodes show a thin red progress bar along the bottom of the thumbnail.
- Unwatched episode rows remain visually unchanged.
- Focus remains the strongest state: the existing focused surface and focus border stay unchanged and readable over all watched states.
- The check icon receives localized English and Arabic accessibility text. No visible `Watched` label is added, keeping rows compact at TV viewing distance.

## Architecture and Data Flow

1. When the episode picker opens, `PlayerViewModel` loads the series episode catalog as it does today.
2. The watch-history repository performs one series-scoped read using the active profile, source, and series identifiers. It returns episode history only, avoiding one network request per episode and avoiding an arbitrary global-history limit.
3. A small pure mapper converts valid history rows into normalized progress values between `0f` and `1f`, keyed by episode content ID.
4. The mapper copies progress into the existing `EpisodeItem.progress` field while constructing the picker seasons. No parallel picker model or database schema is introduced.
5. `PlayerEpisodeSidebar` passes each episode's progress into `PlayerEpisodeSidebarRow`.
6. The row derives `isWatched` from the shared 95% watch-history threshold and renders the appropriate check or progress bar.

The repository interface will expose a series-scoped episode-history read. Supabase will filter by profile, source, series, and `EPISODE` content type. The local implementation will apply the same filtering to its in-memory history. The routing repository will preserve its existing cloud/local fallback and status-reporting behavior.

## State and Refresh Behavior

- Watch state is loaded whenever the picker opens, so reopening it reflects newly saved progress.
- Picker loading completes after both the episode catalog and watch-state lookup finish.
- Choosing an episode keeps the existing playback and dismissal behavior.
- This feature does not alter resume selection, progress persistence, autoplay, or catalog synchronization.

## Failure Handling

- Episode catalog failure follows the existing empty/loading behavior.
- Watch-history read failure is non-fatal: the picker still displays every available episode without watched highlighting.
- History rows for another source or series are ignored even if provider IDs collide.
- History rows whose content IDs no longer exist in the episode catalog are ignored.

## Localization and Accessibility

- Add localized English and Arabic content descriptions for the watched check.
- The icon remains direction-independent and works unchanged in RTL.
- The progress bar follows the row's layout direction naturally; no directional text is introduced.

## Testing

- Pure mapper tests cover unwatched, partially watched, completed, malformed-duration, foreign-source, foreign-series, and missing-catalog episode cases.
- Repository tests cover series/source/profile filtering where practical.
- Player state tests verify that opening the picker exposes normalized episode progress and that history failure still exposes the catalog.
- UI/component tests or compile coverage verify that the row receives progress while preserving the current-playing state.
- Run focused tests first, followed by all JVM tests, `lintDebug`, and `assembleDebug`.

## Out of Scope

- Editing or clearing watch history from the picker.
- A visible watched legend or watched/unwatched filter.
- Changing the 95% completion threshold.
- Persisting watched state in the catalog database.
- Adding Supabase schema changes.
