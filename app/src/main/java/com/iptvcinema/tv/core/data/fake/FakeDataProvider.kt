package com.iptvcinema.tv.core.data.fake

import com.iptvcinema.tv.core.design.components.ChannelTileData
import com.iptvcinema.tv.core.design.components.PosterCardData
import com.iptvcinema.tv.core.epg.GuideLayoutHelper
import com.iptvcinema.tv.core.model.AccountSummary
import com.iptvcinema.tv.core.model.CastMember
import com.iptvcinema.tv.core.model.ChannelItem
import com.iptvcinema.tv.core.model.EpgProgram
import com.iptvcinema.tv.core.model.EpisodeItem
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.PlaylistSourceItem
import com.iptvcinema.tv.core.model.ProfileType
import com.iptvcinema.tv.core.model.SearchResults
import com.iptvcinema.tv.core.model.SeasonItem
import com.iptvcinema.tv.core.model.SeriesItem
import com.iptvcinema.tv.core.model.SourceStatus
import com.iptvcinema.tv.core.model.SourceType
import com.iptvcinema.tv.core.model.UserProfile
import com.iptvcinema.tv.core.model.ValidationStatus

object FakeDataProvider {

    const val ACTIVATION_CODE = "C7K4-9P"

    val profiles = listOf(
        UserProfile("1", "Main", ProfileType.MAIN, "M"),
        UserProfile("2", "Family", ProfileType.FAMILY, "F"),
        UserProfile("3", "Kids", ProfileType.KIDS, "K"),
        UserProfile("4", "Guest", ProfileType.GUEST, "G"),
    )

    val accountSummary = AccountSummary(
        name = "Alex Rivera",
        email = "alex@example.com",
        plan = "Premium Plan",
        renewalDate = "July 15, 2026",
    )

    val featuredHero = MovieItem(
        id = "hero-1",
        title = "Shadows of Aurora",
        year = 2024,
        runtimeMinutes = 117,
        rating = "8.4",
        plot = "A lone explorer discovers a hidden world beneath the northern lights, where ancient secrets threaten to reshape humanity forever.",
        genres = listOf("Action", "Adventure"),
        is4K = true,
        imageUrl = FakeImageUrls.HERO_BACKDROP,
        backdropUrl = FakeImageUrls.HERO_BACKDROP,
    )

    val movies = listOf(
        featuredHero,
        MovieItem("m1", "Beyond the Horizon", 2024, 135, "8.1", "A crew ventures beyond known space.", listOf("Sci-Fi", "Adventure"), is4K = true, imageUrl = FakeImageUrls.MOVIE_BEYOND_HORIZON, backdropUrl = FakeImageUrls.MOVIE_BEYOND_HORIZON),
        MovieItem("m2", "Dragon's Oath", 2024, 138, "8.7", "A warrior binds their fate to an ancient dragon.", listOf("Fantasy", "Adventure"), is4K = true, imageUrl = FakeImageUrls.MOVIE_DRAGONS_OATH, backdropUrl = FakeImageUrls.MOVIE_DRAGONS_OATH),
        MovieItem("m3", "Midnight Signal", 2023, 108, "7.6", "Radio waves from the past rewrite the present.", listOf("Thriller"), progress = 0.72f, imageUrl = FakeImageUrls.MOVIE_MIDNIGHT_SIGNAL, backdropUrl = FakeImageUrls.MOVIE_MIDNIGHT_SIGNAL),
        MovieItem("m4", "Desert Echo", 2025, 125, "7.9", "Survival across shifting dunes.", listOf("Drama"), is4K = true, imageUrl = FakeImageUrls.MOVIE_DESERT_ECHO, backdropUrl = FakeImageUrls.MOVIE_DESERT_ECHO),
        MovieItem("m5", "Northern Lights", 2022, 92, "7.4", "A family reunion under polar skies.", listOf("Family"), isFavorite = true, imageUrl = FakeImageUrls.MOVIE_NORTHERN_LIGHTS),
        MovieItem("m6", "Silent Harbor", 2024, 150, "8.0", "A port town hides a conspiracy.", listOf("Mystery"), imageUrl = FakeImageUrls.MOVIE_SILENT_HARBOR),
        MovieItem("m7", "Crimson Tide", 2023, 112, "7.5", "Naval drama on the high seas.", listOf("Action"), imageUrl = FakeImageUrls.MOVIE_CRIMSON_TIDE),
        MovieItem("m8", "Whisper Valley", 2024, 98, "7.8", "Voices echo through an abandoned valley.", listOf("Horror"), is4K = true, imageUrl = FakeImageUrls.MOVIE_WHISPER_VALLEY),
    )

    val featuredSeries = SeriesItem(
        id = "s-hero",
        title = "The Last Kingdoms",
        year = 2024,
        rating = "8.5",
        plot = "Rival kingdoms clash as an ancient prophecy awakens.",
        genres = listOf("Fantasy", "Drama"),
        seasonCount = 2,
        is4K = true,
        imageUrl = FakeImageUrls.SERIES_KINGDOM,
        backdropUrl = FakeImageUrls.SERIES_KINGDOM,
    )

    val seriesList = listOf(
        featuredSeries,
        SeriesItem("s1", "Kingdom Fallen", 2024, "8.5", "Three seasons of betrayal and redemption.", listOf("Drama", "Fantasy"), 3, is4K = true, seasons = kingdomFallenSeasons(), imageUrl = FakeImageUrls.SERIES_KINGDOM, backdropUrl = FakeImageUrls.SERIES_KINGDOM),
        SeriesItem("s2", "Neon District", 2023, "7.9", "Cyber detectives patrol a neon city.", listOf("Sci-Fi"), 2, hasNewEpisode = true, imageUrl = FakeImageUrls.SERIES_NEON, backdropUrl = FakeImageUrls.SERIES_NEON),
        SeriesItem("s3", "Desert Crown", 2024, "8.0", "Royal intrigue in the dunes.", listOf("Drama"), 1, is4K = true, imageUrl = FakeImageUrls.MOVIE_DESERT_ECHO),
        SeriesItem("s4", "Ocean's Edge", 2022, "7.6", "Coastal families navigate change.", listOf("Drama"), 4, progress = 0.45f, imageUrl = FakeImageUrls.MOVIE_SILENT_HARBOR),
        SeriesItem("s5", "Starbound", 2025, "8.2", "A colony ship faces the unknown.", listOf("Sci-Fi"), 1, hasNewEpisode = true, is4K = true, imageUrl = FakeImageUrls.MOVIE_BEYOND_HORIZON),
        SeriesItem("s6", "Hidden Path", 2023, "7.7", "Secrets buried in mountain trails.", listOf("Mystery"), 2, imageUrl = FakeImageUrls.MOVIE_WHISPER_VALLEY),
    )

    val channels = listOf(
        ChannelItem("c1", "Nature World HD", FakeImageUrls.LIVE_NATURE, 101, "Documentary", "Wild Kingdoms", "Explore the untamed beauty of wildlife across continents.", "10:00 AM", "11:00 AM", 0.45f, "4K"),
        ChannelItem("c2", "News One", FakeImageUrls.LIVE_NEWS, 102, "News", "Evening Report", "Breaking news and in-depth analysis from around the world.", "9:30 AM", "10:30 AM", 0.7f, "HD"),
        ChannelItem("c3", "Sports Live", FakeImageUrls.LIVE_SPORTS, 103, "Sports", "Match Highlights", "Relive the best moments from today's top sporting events.", "10:00 AM", "12:00 PM", 0.2f, "4K"),
        ChannelItem("c4", "Family Channel", FakeImageUrls.LIVE_FAMILY, 104, "Kids", "Animated Stories", "Wholesome animated adventures for the whole family.", "9:00 AM", "10:00 AM", 0.8f, "HD"),
        ChannelItem("c5", "Documentary Plus", FakeImageUrls.LIVE_OCEAN, 105, "Documentary", "Ocean Depths", "A deep dive into the mysteries of the ocean.", "10:15 AM", "11:15 AM", 0.55f),
        ChannelItem("c6", "Cinema Classic", FakeImageUrls.LIVE_CINEMA, 106, "Movies", "Golden Era Films", "Timeless cinema from the golden age of Hollywood.", "10:00 AM", "12:30 PM", 0.35f, "HD"),
        ChannelItem("c7", "World News 24", FakeImageUrls.LIVE_NEWS, 107, "News", "Global Briefing", "Round-the-clock international news coverage.", "10:00 AM", "11:00 AM", 0.5f),
        ChannelItem("c8", "Lifestyle HD", FakeImageUrls.LIVE_LIFESTYLE, 108, "Lifestyle", "Home & Garden", "Inspiration for modern living and outdoor spaces.", "9:45 AM", "10:45 AM", 0.65f, "HD"),
        ChannelItem("error", "Stream Error Demo", FakeImageUrls.LIVE_CINEMA, 109, "Movies", "Unavailable", "Demo channel that simulates a playback error.", "10:00 AM", "11:00 AM", 0f, "HD"),
    )

    val epgPrograms: List<EpgProgram> = buildEpgPrograms()

    val playlistSources = listOf(
        PlaylistSourceItem("ps1", "Family IPTV", SourceType.XTREAM_CODES, SourceStatus.ACTIVE, 1240, lastSynced = "12 min ago"),
        PlaylistSourceItem("ps2", "Sports Pack", SourceType.M3U, SourceStatus.NEEDS_ATTENTION, 320, lastSynced = "2 hours ago", epgAvailable = false),
        PlaylistSourceItem("ps3", "Kids Playlist", SourceType.M3U, SourceStatus.ACTIVE, 85, lastSynced = "1 day ago"),
    )

    val cast = listOf(
        CastMember("Elena Voss", "Lead"),
        CastMember("Marcus Chen", "Supporting"),
        CastMember("Sofia Al-Rashid", "Supporting"),
        CastMember("James Okonkwo", "Guest"),
    )

    val languages = listOf("English", "Arabic", "Hindi", "Spanish", "French")
    val subtitles = listOf("English", "Arabic", "Hindi", "Spanish", "French", "Turkish")

    val movieCategories = listOf("All", "Action", "Drama", "Arabic", "Family", "4K", "New", "Top Rated")
    val seriesCategories = listOf("All", "Drama", "Action", "Arabic", "Kids", "New Episodes", "4K", "Top Rated")
    val liveCategories = listOf("All Channels", "News", "Sports", "Movies", "Kids", "Entertainment", "Documentary", "Lifestyle")
    val myListCategories = listOf("All", "Movies", "Series", "Channels", "Continue Watching")
    val searchFilters = listOf("All", "Movies", "Series", "Live TV", "Channels")
    val recentSearches = listOf("shadow", "eclipse", "ocean", "kingdom", "lost city")

    val xtreamValidation = listOf(
        ValidationStatus("Server Reachable", true),
        ValidationStatus("Account Active", true),
        ValidationStatus("Channels Found", true),
        ValidationStatus("EPG Available", true),
    )

    val m3uPreview = listOf(
        ValidationStatus("Playlist validation", true),
        ValidationStatus("Channels found", true),
        ValidationStatus("Groups detected", true),
        ValidationStatus("EPG match rate", false),
    )

    val settingsSections = listOf(
        "Account", "Subscription", "Playback", "Language",
        "Parental Controls", "Notifications", "Device Preferences", "About",
    )

    val ratingOptions = listOf("G", "PG", "12+", "16+", "18+")

    val blockedCategories = listOf("Adult", "Pay-Per-View", "Uncategorized")

    val arabicPicks = listOf(
        PosterCardData(title = "أمل بعيد", year = "2024", runtime = "2h 5m", is4K = true, imageUrl = FakeImageUrls.MOVIE_DESERT_ECHO),
        PosterCardData(title = "مدينة الليل", year = "2023", runtime = "1h 48m", is4K = false, imageUrl = FakeImageUrls.MOVIE_SILENT_HARBOR),
        PosterCardData(title = "رحلة الصحراء", year = "2024", runtime = "2h 12m", is4K = true, imageUrl = FakeImageUrls.MOVIE_DESERT_ECHO),
        PosterCardData(title = "أسرار البحر", year = "2022", runtime = "1h 55m", is4K = false, imageUrl = FakeImageUrls.MOVIE_WHISPER_VALLEY),
    )

    val newReleases = listOf(
        PosterCardData(title = "Neon Horizon", year = "2025", runtime = "2h 8m", is4K = true, imageUrl = FakeImageUrls.MOVIE_BEYOND_HORIZON),
        PosterCardData(title = "Last Signal", year = "2025", runtime = "1h 42m", is4K = true, imageUrl = FakeImageUrls.MOVIE_MIDNIGHT_SIGNAL),
        PosterCardData(title = "Frozen Path", year = "2025", runtime = "1h 56m", is4K = false, imageUrl = FakeImageUrls.MOVIE_NORTHERN_LIGHTS),
        PosterCardData(title = "City of Glass", year = "2025", runtime = "2h 20m", is4K = true, imageUrl = FakeImageUrls.MOVIE_DRAGONS_OATH),
        PosterCardData(title = "Silent Storm", year = "2025", runtime = "1h 38m", is4K = false, imageUrl = FakeImageUrls.MOVIE_WHISPER_VALLEY),
    )

    /** Maps Arabic pick / new release titles to movie IDs for navigation. */
    val arabicPickMovieIds = mapOf(
        "أمل بعيد" to "m4",
        "مدينة الليل" to "m6",
        "رحلة الصحراء" to "m4",
        "أسرار البحر" to "m8",
    )

    val newReleaseMovieIds = mapOf(
        "Neon Horizon" to "m1",
        "Last Signal" to "m3",
        "Frozen Path" to "m7",
        "City of Glass" to "m2",
        "Silent Storm" to "m8",
    )

    fun movieById(id: String): MovieItem? = movies.find { it.id == id }

    fun seriesById(id: String): SeriesItem? = seriesList.find { it.id == id }?.let { series ->
        if (series.seasons.isEmpty() && series.id == "s1") {
            series.copy(seasons = kingdomFallenSeasons())
        } else {
            series
        }
    }

    fun searchResults(query: String, filter: String = "All"): SearchResults {
        val q = query.lowercase()
        val allResults = SearchResults(
            movies = movies.filter { movieMatchesQuery(movie = it, query = q) },
            series = seriesList.filter { seriesMatchesQuery(series = it, query = q) },
            channels = channels.filter { it.name.lowercase().contains(q) || it.currentProgram.lowercase().contains(q) },
        )
        return when (filter) {
            "Movies" -> allResults.copy(series = emptyList(), channels = emptyList())
            "Series" -> allResults.copy(movies = emptyList(), channels = emptyList())
            "Live TV", "Channels" -> allResults.copy(movies = emptyList(), series = emptyList())
            else -> allResults
        }
    }

    private fun movieMatchesQuery(movie: MovieItem, query: String): Boolean {
        if (movie.title.lowercase().contains(query)) return true
        return demoMovieCastNames(movie.id).any { it.lowercase().contains(query) }
    }

    private fun seriesMatchesQuery(series: SeriesItem, query: String): Boolean {
        if (series.title.lowercase().contains(query)) return true
        return demoSeriesCastNames(series.id).any { it.lowercase().contains(query) }
    }

    private fun demoMovieCastNames(movieId: String): List<String> = when (movieId) {
        "hero-1", "m3", "m6" -> listOf("Elena Voss", "Marcus Chen")
        "m2" -> listOf("Sofia Al-Rashid", "James Okonkwo")
        "m4", "m5" -> listOf("Marcus Chen")
        else -> emptyList()
    }

    private fun demoSeriesCastNames(seriesId: String): List<String> = when (seriesId) {
        "s-hero", "s1", "s3" -> listOf("Sofia Al-Rashid", "James Okonkwo")
        "s2", "s5" -> listOf("Elena Voss", "Marcus Chen")
        "s4" -> listOf("Marcus Chen")
        else -> emptyList()
    }

    fun channelsForCategory(category: String): List<ChannelItem> {
        if (category == "All Channels") return channels
        val mappedCategory = when (category) {
            "Entertainment" -> "Lifestyle"
            else -> category
        }
        return channels.filter { it.category.equals(mappedCategory, ignoreCase = true) }
    }

    fun epgForChannels(
        visibleChannels: List<ChannelItem>,
        windowStartMs: Long = GuideLayoutHelper.defaultWindowStart(System.currentTimeMillis()),
        windowEndMs: Long = GuideLayoutHelper.defaultWindowEnd(System.currentTimeMillis()),
    ): List<EpgProgram> {
        val channelIds = visibleChannels.map { it.id }.toSet()
        return epgPrograms.filter { program ->
            program.channelId in channelIds &&
                program.endEpochMs > windowStartMs &&
                program.startEpochMs < windowEndMs
        }
    }

    fun toPosterCardData(movie: MovieItem) = PosterCardData(
        title = movie.title,
        year = movie.year.toString(),
        runtime = formatRuntime(movie.runtimeMinutes),
        is4K = movie.is4K,
        progress = movie.progress,
        isFavorite = movie.isFavorite,
        imageUrl = movie.imageUrl,
        contentId = movie.id,
    )

    fun toPosterCardData(series: SeriesItem) = PosterCardData(
        title = series.title,
        year = series.year.toString(),
        runtime = "${series.seasonCount} Seasons",
        is4K = series.is4K,
        progress = series.progress,
        isFavorite = series.isFavorite,
        imageUrl = series.imageUrl,
        contentId = series.id,
    )

    fun toChannelTileData(channel: ChannelItem) = ChannelTileData(
        id = channel.id,
        channelName = channel.name,
        logoUrl = channel.logoUrl,
        currentProgram = channel.currentProgram,
        qualityBadge = channel.qualityBadge,
        programProgress = channel.programProgress,
    )

    fun samplePosters(): List<PosterCardData> = movies.drop(1).map { toPosterCardData(it) }

    fun sampleChannels(): List<ChannelTileData> = channels.map { toChannelTileData(it) }

    fun continueWatchingMovies(): List<MovieItem> = movies.filter { it.progress != null }

    fun continueWatchingSeries(): List<SeriesItem> = seriesList.filter { it.progress != null }

    fun myListItems(): List<PosterCardData> = myListItemsForCategory("All")

    fun myListItemsForCategory(category: String): List<PosterCardData> = when (category) {
        "Movies" -> movies.filter { it.isFavorite || it.progress != null }.map { toPosterCardData(it) }
        "Series" -> seriesList.filter { it.isFavorite || it.progress != null }.map { toPosterCardData(it) }
        "Continue Watching" ->
            movies.filter { it.progress != null }.map { toPosterCardData(it) } +
                seriesList.filter { it.progress != null }.map { toPosterCardData(it) }
        "Channels" -> emptyList()
        else ->
            movies.filter { it.isFavorite || it.progress != null }.map { toPosterCardData(it) } +
                seriesList.filter { it.isFavorite || it.progress != null }.map { toPosterCardData(it) }
    }

    fun moviesForCategory(category: String): List<MovieItem> {
        if (category == "All") return movies
        return when (category) {
            "4K" -> movies.filter { it.is4K }
            "New" -> movies.filter { it.year >= 2024 }
            "Sports" -> movies.filter { it.genres.any { g -> g.equals("Action", ignoreCase = true) } }
            else -> movies.filter { movie ->
                movie.genres.any { it.equals(category, ignoreCase = true) } ||
                    movie.title.contains(category, ignoreCase = true)
            }
        }
    }

    fun recentlyAddedMovies(limit: Int = 20): List<MovieItem> = movies.takeLast(limit).reversed()

    fun seriesForCategory(category: String): List<SeriesItem> {
        if (category == "All") return seriesList
        return when (category) {
            "4K" -> seriesList.filter { it.is4K }
            "New Episodes" -> seriesList.filter { it.hasNewEpisode }
            "Kids" -> seriesList.filter { it.genres.any { g -> g.equals("Family", ignoreCase = true) } }
            else -> seriesList.filter { series ->
                series.genres.any { it.equals(category, ignoreCase = true) } ||
                    series.title.contains(category, ignoreCase = true)
            }
        }
    }

    fun categoryIndex(categories: List<String>, filter: String): Int {
        if (filter.isBlank()) return 0
        return categories.indexOfFirst { it.equals(filter, ignoreCase = true) }.coerceAtLeast(0)
    }

    private fun formatRuntime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    private fun kingdomFallenSeasons(): List<SeasonItem> = listOf(
        SeasonItem("sn1", 1, listOf(
            EpisodeItem("e1", 1, "The Fall", 52),
            EpisodeItem("e2", 2, "Broken Crown", 48, 0.3f),
            EpisodeItem("e3", 3, "Alliance", 50),
            EpisodeItem("e4", 4, "Siege", 55),
        )),
        SeasonItem("sn2", 2, listOf(
            EpisodeItem("e5", 1, "Return", 49),
            EpisodeItem("e6", 2, "Betrayal", 51),
            EpisodeItem("e7", 3, "Reckoning", 54),
        )),
        SeasonItem("sn3", 3, listOf(
            EpisodeItem("e8", 1, "Dawn", 47),
            EpisodeItem("e9", 2, "Legacy", 52),
        )),
    )

    private fun buildEpgPrograms(): List<EpgProgram> {
        val programs = mutableListOf<EpgProgram>()
        val titles = listOf(
            "Morning News", "Wild Kingdoms", "Match Day", "Cartoon Hour",
            "Ocean Depths", "Classic Film", "Global Briefing", "Home & Garden",
            "Talk Show", "Documentary Special", "Live Sports", "Evening Movie",
        )
        val nowMs = System.currentTimeMillis()
        val windowStart = GuideLayoutHelper.defaultWindowStart(nowMs)
        channels.forEachIndexed { channelIndex, channel ->
            var slotStartMs = windowStart
            repeat(8) { slot ->
                val durationMinutes = if (slot % 2 == 0) 60 else 90
                val durationMs = durationMinutes * 60_000L
                val slotEndMs = slotStartMs + durationMs
                val startInstant = java.time.Instant.ofEpochMilli(slotStartMs)
                    .atZone(java.time.ZoneId.systemDefault())
                programs.add(
                    EpgProgram(
                        id = "epg-${channel.id}-$slot",
                        channelId = channel.id,
                        title = if (slot == 1) channel.currentProgram else titles[(channelIndex + slot) % titles.size],
                        startHour = startInstant.hour,
                        startMinute = startInstant.minute,
                        durationMinutes = durationMinutes,
                        startEpochMs = slotStartMs,
                        endEpochMs = slotEndMs,
                        description = "Demo program description for ${channel.name}.",
                    ),
                )
                slotStartMs = slotEndMs
            }
        }
        return programs
    }
}
