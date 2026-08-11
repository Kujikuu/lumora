# Episode Picker Watched-State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Highlight completed episodes and show partial progress in the player's Choose episode sidebar using the active profile's persisted watch history.

**Architecture:** Add one profile/source/series-scoped watch-history read, then combine it with the episode catalog in a small testable loader. Normalize persisted positions into the existing `EpisodeItem.progress` field, and keep the Compose row responsible only for rendering `UNWATCHED`, `PARTIAL`, and `WATCHED` states.

**Tech Stack:** Kotlin, coroutines/Flow, Supabase PostgREST, Hilt, Jetpack Compose for TV, JUnit 4, Gradle.

## Global Constraints

- Watched means valid saved progress greater than or equal to 95% of duration.
- Partial means positive valid progress below 95%; invalid or missing durations remain unwatched.
- Watched rows show a green circular check and slightly muted thumbnail; partial rows show a thin red progress bar.
- Current-playing and TV focus treatments remain stronger than watched styling.
- History failures are non-fatal and must not hide catalog episodes.
- Profile, source, series, and `EPISODE` content type must all match.
- Add English and Arabic accessibility copy and preserve RTL behavior.
- Do not add Room or Supabase schema changes.
- Do not stage unrelated Supabase edits, `.kotlin/errors` logs, or uncommitted resume-fix files unless a task names a file.

## File Structure

- `WatchHistoryRepository.kt` and its local/Supabase/routing implementations own the scoped history read.
- `EpisodeWatchProgress.kt` owns validation, normalization, the threshold, and visual classification.
- `EpisodePickerWatchStateMapper.kt` maps history into existing episode models.
- `EpisodePickerLoader.kt` orchestrates catalog and history reads without coupling tests to a ViewModel.
- `PlayerViewModel.kt` invokes the loader when the picker opens.
- `PlayerComponents.kt` and `DetailComponents.kt` pass and render progress.
- English and Arabic resources provide watched-marker accessibility text.

---

### Task 1: Add one efficient series-scoped history read

**Files:**
- Modify: `app/src/main/java/com/iptvcinema/tv/core/data/repository/WatchHistoryRepository.kt:7-28`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/data/repository/local/LocalUserDataRepositories.kt:144-243`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/data/repository/supabase/SupabaseWatchHistoryRepository.kt:95-175,259-269`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/data/repository/RoutingUserDataRepositories.kt:128-177`
- Create: `app/src/test/java/com/iptvcinema/tv/core/data/repository/WatchHistorySeriesFilterTest.kt`

**Interfaces:**
- Produces: `getEpisodeHistoryForSeries(profileId: String, sourceId: String, seriesId: String): List<WatchHistoryItem>`.
- Produces: `filterEpisodeHistoryForSeries(items, sourceId, seriesId)` for local filtering and testing.

- [ ] **Step 1: Write the failing scoped-filter test**

```kotlin
@Test
fun filterEpisodeHistoryForSeries_keepsOnlyMatchingEpisodesNewestFirst() {
    val result = filterEpisodeHistoryForSeries(
        items = listOf(
            history("old", "source-a", "series-a", WatchHistoryContentType.EPISODE, watchedAt = 100),
            history("wrong-source", "source-b", "series-a", WatchHistoryContentType.EPISODE, watchedAt = 400),
            history("wrong-series", "source-a", "series-b", WatchHistoryContentType.EPISODE, watchedAt = 300),
            history("movie", "source-a", "series-a", WatchHistoryContentType.MOVIE, watchedAt = 500),
            history("new", "source-a", "series-a", WatchHistoryContentType.EPISODE, watchedAt = 200),
        ),
        sourceId = "source-a",
        seriesId = "series-a",
    )
    assertEquals(listOf("new", "old"), result.map { it.contentId })
}

private fun history(
    contentId: String,
    sourceId: String,
    seriesId: String,
    contentType: WatchHistoryContentType,
    watchedAt: Long,
) = WatchHistoryItem(
    id = contentId,
    profileId = "profile-a",
    sourceId = sourceId,
    contentId = contentId,
    contentType = contentType,
    seriesId = seriesId,
    title = contentId,
    posterUrl = null,
    positionMs = 30_000L,
    durationMs = 60_000L,
    lastWatchedAt = Instant.ofEpochSecond(watchedAt),
)
```

- [ ] **Step 2: Run RED**

```bash
./gradlew testDebugUnitTest --tests com.iptvcinema.tv.core.data.repository.WatchHistorySeriesFilterTest
```

Expected: compilation fails because `filterEpisodeHistoryForSeries` does not exist.

- [ ] **Step 3: Add the contract and local filter**

```kotlin
suspend fun getEpisodeHistoryForSeries(
    profileId: String,
    sourceId: String,
    seriesId: String,
): List<WatchHistoryItem>

internal fun filterEpisodeHistoryForSeries(
    items: List<WatchHistoryItem>,
    sourceId: String,
    seriesId: String,
): List<WatchHistoryItem> = items
    .asSequence()
    .filter { it.contentType == WatchHistoryContentType.EPISODE }
    .filter { it.sourceId == sourceId && it.seriesId == seriesId }
    .sortedByDescending { it.lastWatchedAt }
    .toList()
```

Use the helper inside `LocalWatchHistoryRepository` under its mutex.

```kotlin
override suspend fun getEpisodeHistoryForSeries(
    profileId: String,
    sourceId: String,
    seriesId: String,
): List<WatchHistoryItem> = mutex.withLock {
    filterEpisodeHistoryForSeries(historyByProfile[profileId].orEmpty(), sourceId, seriesId)
}
```

- [ ] **Step 4: Implement Supabase and routing variants**

Add `COLUMN_SOURCE_ID = "source_id"` and issue one Supabase request:

```kotlin
override suspend fun getEpisodeHistoryForSeries(
    profileId: String,
    sourceId: String,
    seriesId: String,
): List<WatchHistoryItem> = supabaseClient.from(TABLE)
    .select(Columns.ALL) {
        filter {
            eq(COLUMN_PROFILE_ID, profileId)
            eq(COLUMN_SOURCE_ID, sourceId)
            eq(COLUMN_SERIES_ID, seriesId)
            eq(COLUMN_CONTENT_TYPE, WatchHistoryContentType.EPISODE.name)
        }
        order(COLUMN_LAST_WATCHED_AT, Order.DESCENDING)
    }
    .decodeList<WatchHistoryDto>()
    .map { it.toDomain() }
```

The routing method preserves cloud health reporting:

```kotlin
override suspend fun getEpisodeHistoryForSeries(
    profileId: String,
    sourceId: String,
    seriesId: String,
): List<WatchHistoryItem> {
    val backend = resolveBackend()
    return runCatching {
        backend.getEpisodeHistoryForSeries(profileId, sourceId, seriesId)
    }.onFailure { cloudAccountStatus.reportCloudReadFailure() }
        .onSuccess { cloudAccountStatus.reportCloudReadSuccess() }
        .getOrDefault(emptyList())
}
```

- [ ] **Step 5: Verify GREEN and compile**

```bash
./gradlew testDebugUnitTest --tests com.iptvcinema.tv.core.data.repository.WatchHistorySeriesFilterTest compileDebugKotlin
```

- [ ] **Step 6: Commit only Task 1 files**

```bash
git add app/src/main/java/com/iptvcinema/tv/core/data/repository/WatchHistoryRepository.kt app/src/main/java/com/iptvcinema/tv/core/data/repository/local/LocalUserDataRepositories.kt app/src/main/java/com/iptvcinema/tv/core/data/repository/supabase/SupabaseWatchHistoryRepository.kt app/src/main/java/com/iptvcinema/tv/core/data/repository/RoutingUserDataRepositories.kt app/src/test/java/com/iptvcinema/tv/core/data/repository/WatchHistorySeriesFilterTest.kt
git commit -m "feat(history): query episode progress by series"
```

### Task 2: Map persisted positions into episode progress

**Files:**
- Create: `app/src/main/java/com/iptvcinema/tv/core/player/EpisodeWatchProgress.kt`
- Create: `app/src/main/java/com/iptvcinema/tv/features/player/EpisodePickerWatchStateMapper.kt`
- Create: `app/src/test/java/com/iptvcinema/tv/features/player/EpisodePickerWatchStateMapperTest.kt`

**Interfaces:**
- Consumes: scoped `List<WatchHistoryItem>` from Task 1.
- Produces: `EpisodeWatchProgress.normalized(positionMs, durationMs): Float?` and `WATCHED_THRESHOLD = 0.95f`.
- Produces: `EpisodePickerWatchStateMapper.apply(seasons, history, sourceId, seriesId): List<SeasonItem>`.

- [ ] **Step 1: Write failing mapper tests**

```kotlin
@Test
fun apply_mapsPartialAndCompletedProgress_withoutChangingUnwatchedEpisodes() {
    val seasons = seasons("partial", "watched", "unwatched")
    val history = listOf(
        history("partial", "source-a", "series-a", 25_000L, 100_000L),
        history("watched", "source-a", "series-a", 95_000L, 100_000L),
    )
    val episodes = EpisodePickerWatchStateMapper
        .apply(seasons, history, "source-a", "series-a")
        .flatMap { it.episodes }.associateBy { it.id }
    assertEquals(0.25f, episodes.getValue("partial").progress!!, 0.001f)
    assertEquals(0.95f, episodes.getValue("watched").progress!!, 0.001f)
    assertNull(episodes.getValue("unwatched").progress)
}

@Test
fun apply_ignoresInvalidForeignAndMissingCatalogRows() {
    val seasons = seasons("target")
    val history = listOf(
        history("target", "source-b", "series-a", 50_000L, 100_000L),
        history("target", "source-a", "series-b", 50_000L, 100_000L),
        history("target", "source-a", "series-a", 50_000L, null),
        history("missing", "source-a", "series-a", 50_000L, 100_000L),
    )
    val episode = EpisodePickerWatchStateMapper
        .apply(seasons, history, "source-a", "series-a")
        .single().episodes.single()
    assertNull(episode.progress)
}

private fun seasons(vararg episodeIds: String) = listOf(
    SeasonItem(
        id = "series-a-s1",
        seasonNumber = 1,
        episodes = episodeIds.mapIndexed { index, id ->
            EpisodeItem(id, index + 1, id, durationMinutes = 45)
        },
    ),
)

private fun history(
    contentId: String,
    sourceId: String,
    seriesId: String,
    positionMs: Long,
    durationMs: Long?,
) = WatchHistoryItem(
    id = contentId,
    profileId = "profile-a",
    sourceId = sourceId,
    contentId = contentId,
    contentType = WatchHistoryContentType.EPISODE,
    seriesId = seriesId,
    title = contentId,
    posterUrl = null,
    positionMs = positionMs,
    durationMs = durationMs,
    lastWatchedAt = Instant.parse("2026-08-11T10:00:00Z"),
)
```

- [ ] **Step 2: Run RED**

```bash
./gradlew testDebugUnitTest --tests com.iptvcinema.tv.features.player.EpisodePickerWatchStateMapperTest
```

- [ ] **Step 3: Implement normalized progress**

```kotlin
object EpisodeWatchProgress {
    const val WATCHED_THRESHOLD = 0.95f

    fun normalized(positionMs: Long, durationMs: Long?): Float? {
        val validDuration = durationMs?.takeIf { it > 0L } ?: return null
        if (positionMs <= 0L) return null
        return (positionMs.toFloat() / validDuration.toFloat()).coerceIn(0f, 1f)
    }
}
```

- [ ] **Step 4: Implement the mapper**

```kotlin
internal object EpisodePickerWatchStateMapper {
    fun apply(
        seasons: List<SeasonItem>,
        history: List<WatchHistoryItem>,
        sourceId: String,
        seriesId: String,
    ): List<SeasonItem> {
        val catalogIds = seasons.flatMap { it.episodes }.mapTo(mutableSetOf()) { it.id }
        val latestByEpisodeId = history.asSequence()
            .filter { it.contentType == WatchHistoryContentType.EPISODE }
            .filter { it.sourceId == sourceId && it.seriesId == seriesId }
            .filter { it.contentId in catalogIds }
            .groupBy { it.contentId }
            .mapValues { (_, rows) -> rows.maxBy { it.lastWatchedAt } }
        val progressByEpisodeId = latestByEpisodeId.mapNotNull { (episodeId, item) ->
            EpisodeWatchProgress.normalized(item.positionMs, item.durationMs)
                ?.let { progress -> episodeId to progress }
        }.toMap()

        return seasons.map { season ->
            season.copy(episodes = season.episodes.map { episode ->
                episode.copy(progress = progressByEpisodeId[episode.id])
            })
        }
    }
}
```

- [ ] **Step 5: Verify GREEN**

```bash
./gradlew testDebugUnitTest --tests com.iptvcinema.tv.features.player.EpisodePickerWatchStateMapperTest
```

- [ ] **Step 6: Commit Task 2**

```bash
git add app/src/main/java/com/iptvcinema/tv/core/player/EpisodeWatchProgress.kt app/src/main/java/com/iptvcinema/tv/features/player/EpisodePickerWatchStateMapper.kt app/src/test/java/com/iptvcinema/tv/features/player/EpisodePickerWatchStateMapperTest.kt
git commit -m "feat(player): map episode watch progress"
```

### Task 3: Load watched state when the picker opens

**Files:**
- Create: `app/src/main/java/com/iptvcinema/tv/features/player/EpisodePickerLoader.kt`
- Create: `app/src/test/java/com/iptvcinema/tv/features/player/EpisodePickerLoaderTest.kt`
- Modify: `app/src/main/java/com/iptvcinema/tv/features/player/PlayerViewModel.kt:235-251`

**Interfaces:**
- Consumes: Task 1 repository method and Task 2 mapper.
- Produces: `loadEpisodePickerSeasons(profileId, sourceId, seriesId, loadEpisodes, loadHistory): List<SeasonItem>`.

- [ ] **Step 1: Write failing loader tests**

```kotlin
@Test
fun loadEpisodePickerSeasons_combinesCatalogWithHistory() = runBlocking {
    val result = loadEpisodePickerSeasons(
        profileId = "profile-a",
        sourceId = "source-a",
        seriesId = "series-a",
        loadEpisodes = { _, _ -> listOf(episode("e1")) },
        loadHistory = { _, _, _ -> listOf(history("e1", 50_000L, 100_000L)) },
    )
    assertEquals(0.5f, result.single().episodes.single().progress!!, 0.001f)
}

@Test
fun loadEpisodePickerSeasons_keepsCatalog_whenHistoryFails() = runBlocking {
    val result = loadEpisodePickerSeasons(
        profileId = "profile-a",
        sourceId = "source-a",
        seriesId = "series-a",
        loadEpisodes = { _, _ -> listOf(episode("e1")) },
        loadHistory = { _, _, _ -> error("offline") },
    )
    assertEquals("e1", result.single().episodes.single().id)
    assertNull(result.single().episodes.single().progress)
}

@Test
fun loadEpisodePickerSeasons_returnsEmpty_whenCatalogFails() = runBlocking {
    val result = loadEpisodePickerSeasons(
        profileId = "profile-a",
        sourceId = "source-a",
        seriesId = "series-a",
        loadEpisodes = { _, _ -> error("catalog unavailable") },
        loadHistory = { _, _, _ -> emptyList() },
    )
    assertTrue(result.isEmpty())
}

private fun episode(id: String) = CatalogEpisode(
    id = id,
    sourceId = "source-a",
    seriesId = "series-a",
    seasonNumber = 1,
    episodeNumber = 1,
    title = id,
    streamUrl = "https://example.com/$id.m3u8",
    durationMinutes = 45,
    plot = null,
    thumbnailUrl = null,
)

private fun history(contentId: String, positionMs: Long, durationMs: Long?) = WatchHistoryItem(
    id = contentId,
    profileId = "profile-a",
    sourceId = "source-a",
    contentId = contentId,
    contentType = WatchHistoryContentType.EPISODE,
    seriesId = "series-a",
    title = contentId,
    posterUrl = null,
    positionMs = positionMs,
    durationMs = durationMs,
    lastWatchedAt = Instant.parse("2026-08-11T10:00:00Z"),
)
```

- [ ] **Step 2: Run RED**

```bash
./gradlew testDebugUnitTest --tests com.iptvcinema.tv.features.player.EpisodePickerLoaderTest
```

Expected: compilation fails because `loadEpisodePickerSeasons` does not exist.

- [ ] **Step 3: Implement the loader**

```kotlin
internal suspend fun loadEpisodePickerSeasons(
    profileId: String?,
    sourceId: String,
    seriesId: String,
    loadEpisodes: suspend (String, String) -> List<CatalogEpisode>,
    loadHistory: suspend (String, String, String) -> List<WatchHistoryItem>,
): List<SeasonItem> {
    val episodes = runCatching { loadEpisodes(sourceId, seriesId) }.getOrDefault(emptyList())
    val seasons = SeasonGrouping.toSeasonItems(episodes, seriesId)
    if (profileId.isNullOrBlank() || seasons.isEmpty()) return seasons
    val history = runCatching { loadHistory(profileId, sourceId, seriesId) }.getOrDefault(emptyList())
    return EpisodePickerWatchStateMapper.apply(seasons, history, sourceId, seriesId)
}
```

- [ ] **Step 4: Integrate `PlayerViewModel.openEpisodePicker()`**

Replace direct `SeasonGrouping` usage with:

```kotlin
val profileId = appSessionRepository.sessionState.first().currentProfileId
val seasons = loadEpisodePickerSeasons(
    profileId = profileId,
    sourceId = sourceId,
    seriesId = seriesId,
    loadEpisodes = { resolvedSourceId, resolvedSeriesId ->
        episodeCatalogRepository.getEpisodesForSeries(resolvedSourceId, resolvedSeriesId)
    },
    loadHistory = { resolvedProfileId, resolvedSourceId, resolvedSeriesId ->
        watchHistoryRepository.getEpisodeHistoryForSeries(
            resolvedProfileId,
            resolvedSourceId,
            resolvedSeriesId,
        )
    },
)
_screenState.value = _screenState.value.copy(
    episodePickerSeasons = seasons,
    episodePickerLoading = false,
)
```

Remove the now-unused `SeasonGrouping` import.

- [ ] **Step 5: Verify GREEN with player regression coverage**

```bash
./gradlew testDebugUnitTest --tests com.iptvcinema.tv.features.player.EpisodePickerLoaderTest --tests com.iptvcinema.tv.features.player.EpisodePickerWatchStateMapperTest --tests com.iptvcinema.tv.features.player.PlayerKeyHandlerTest
```

- [ ] **Step 6: Commit Task 3**

```bash
git add app/src/main/java/com/iptvcinema/tv/features/player/EpisodePickerLoader.kt app/src/main/java/com/iptvcinema/tv/features/player/PlayerViewModel.kt app/src/test/java/com/iptvcinema/tv/features/player/EpisodePickerLoaderTest.kt
git commit -m "feat(player): load watched episode state"
```

### Task 4: Render watched and partial states in the TV sidebar

**Files:**
- Modify: `app/src/main/java/com/iptvcinema/tv/core/player/EpisodeWatchProgress.kt`
- Create: `app/src/test/java/com/iptvcinema/tv/core/player/EpisodeWatchProgressTest.kt`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/design/components/PlayerComponents.kt:1339-1354`
- Modify: `app/src/main/java/com/iptvcinema/tv/core/design/components/DetailComponents.kt:688-762`
- Modify: `app/src/main/res/values/strings.xml:340-372`
- Modify: `app/src/main/res/values-ar/strings.xml:325-356`

**Interfaces:**
- Consumes: `EpisodeItem.progress` from Task 3.
- Produces: `EpisodeWatchVisualState` and `EpisodeWatchProgress.visualState(progress)`.
- Changes: `PlayerEpisodeSidebarRow(..., progress: Float?, isPlaying: Boolean, ...)`.

- [ ] **Step 1: Write failing threshold tests**

```kotlin
@Test fun below95Percent_isPartial() =
    assertEquals(EpisodeWatchVisualState.PARTIAL, EpisodeWatchProgress.visualState(0.949f))

@Test fun exactly95Percent_isWatched() =
    assertEquals(EpisodeWatchVisualState.WATCHED, EpisodeWatchProgress.visualState(0.95f))

@Test fun nullAndZero_areUnwatched() {
    assertEquals(EpisodeWatchVisualState.UNWATCHED, EpisodeWatchProgress.visualState(null))
    assertEquals(EpisodeWatchVisualState.UNWATCHED, EpisodeWatchProgress.visualState(0f))
}
```

- [ ] **Step 2: Run RED**

```bash
./gradlew testDebugUnitTest --tests com.iptvcinema.tv.core.player.EpisodeWatchProgressTest
```

- [ ] **Step 3: Implement classification**

```kotlin
enum class EpisodeWatchVisualState { UNWATCHED, PARTIAL, WATCHED }

fun visualState(progress: Float?): EpisodeWatchVisualState = when {
    progress == null || progress <= 0f -> EpisodeWatchVisualState.UNWATCHED
    progress >= WATCHED_THRESHOLD -> EpisodeWatchVisualState.WATCHED
    else -> EpisodeWatchVisualState.PARTIAL
}
```

Add the enum at file scope and the function inside `EpisodeWatchProgress`.

- [ ] **Step 4: Pass progress into the row**

Add `progress = episode.progress` in `PlayerEpisodeSidebar`, and add `progress: Float?` immediately before `isPlaying` in `PlayerEpisodeSidebarRow`.

- [ ] **Step 5: Render the approved visual treatment**

Compute `watchState = EpisodeWatchProgress.visualState(progress)`. For `WATCHED`, apply image alpha `0.62f` and overlay a 26dp `CinemaColors.Success` circle at `Alignment.TopEnd` with an 18dp `Icons.Default.Check`. For `PARTIAL`, overlay a 4dp-high `CinemaColors.Accent` bar at `Alignment.BottomStart` with `fillMaxWidth(progress.coerceIn(0f, 1f))`. Move the duration badge 4dp above the partial bar. Do not alter row focus, click, current-episode focus requester, or current-playing colors.

Use `stringResource(R.string.player_episode_watched)` as the check icon content description.

```kotlin
val watchState = EpisodeWatchProgress.visualState(progress)
val watchedDescription = stringResource(R.string.player_episode_watched)

CinemaAsyncImage(
    imageUrl = thumbnailUrl ?: fallbackImageUrl,
    contentDescription = title,
    modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            alpha = if (watchState == EpisodeWatchVisualState.WATCHED) 0.62f else 1f
        },
    contentScale = ContentScale.Crop,
    fallbackLabel = episodeNumber.toString(),
)

if (watchState == EpisodeWatchVisualState.PARTIAL) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth(progress?.coerceIn(0f, 1f) ?: 0f)
            .height(4.dp)
            .background(CinemaColors.Accent),
    )
}

if (watchState == EpisodeWatchVisualState.WATCHED) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(26.dp)
            .background(CinemaColors.Success, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = watchedDescription,
            tint = CinemaColors.White,
            modifier = Modifier.size(18.dp),
        )
    }
}
```

Keep the existing duration badge and add this modifier before its background:

```kotlin
.padding(bottom = if (watchState == EpisodeWatchVisualState.PARTIAL) 4.dp else 0.dp)
```

- [ ] **Step 6: Add localized accessibility text**

```xml
<!-- values/strings.xml -->
<string name="player_episode_watched">Watched</string>

<!-- values-ar/strings.xml -->
<string name="player_episode_watched">تمت المشاهدة</string>
```

- [ ] **Step 7: Verify GREEN and Compose compilation**

```bash
./gradlew testDebugUnitTest --tests com.iptvcinema.tv.core.player.EpisodeWatchProgressTest --tests com.iptvcinema.tv.features.player.EpisodePickerWatchStateMapperTest --tests com.iptvcinema.tv.features.player.EpisodePickerLoaderTest compileDebugKotlin
```

- [ ] **Step 8: Commit Task 4**

```bash
git add app/src/main/java/com/iptvcinema/tv/core/player/EpisodeWatchProgress.kt app/src/main/java/com/iptvcinema/tv/core/design/components/PlayerComponents.kt app/src/main/java/com/iptvcinema/tv/core/design/components/DetailComponents.kt app/src/main/res/values/strings.xml app/src/main/res/values-ar/strings.xml app/src/test/java/com/iptvcinema/tv/core/player/EpisodeWatchProgressTest.kt
git commit -m "feat(player): highlight watched episodes"
```

### Task 5: Verify automation, isolation, RTL, and the connected TV

**Files:**
- Verify only; do not add production files.

**Interfaces:**
- Consumes: Tasks 1-4.
- Produces: test/build/device evidence and a scoped handoff.

- [ ] **Step 1: Run the complete automated gate**

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug compileDebugAndroidTestKotlin
```

Expected: zero test/lint failures and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 2: Audit patch isolation**

```bash
git diff --check
git status --short
git log -6 --oneline
```

Expected: watched-state commits contain only named files; unrelated changes and logs remain unstaged.

- [ ] **Step 3: Install without clearing TV data**

```bash
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.iptvcinema.tv/.app.MainActivity
```

Expected: `192.168.1.8:5555` is connected, installation succeeds, and existing profile/history remains.

- [ ] **Step 4: Verify English behavior on the TV**

Open a series with completed and partial history, start playback, and open **Choose episode**. Confirm completed episodes show a green check/muted thumbnail; partial episodes show proportional red progress; unwatched episodes have neither; current episode retains its red playing label and initial focus; D-pad focus remains strongest; every row remains selectable; reopening refreshes state.

- [ ] **Step 5: Verify Arabic RTL behavior**

Switch to Arabic and confirm panel position, text, season chips, duration badge, progress direction, and top-end check mirror without overlap or truncation.

- [ ] **Step 6: Capture final state**

```bash
git status --short
git log -5 --oneline
```

Report the exact Gradle result, TV installation result, English/Arabic observations, watched-state commit hashes, and unrelated files left unstaged. Do not push unless the user explicitly asks.
