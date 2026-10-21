package com.iptvcinema.tv.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.iptvcinema.tv.core.database.IptvDatabase
import com.iptvcinema.tv.core.database.dao.UserDataCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideIptvDatabase(
        @ApplicationContext context: Context,
    ): IptvDatabase = Room.databaseBuilder(
        context,
        IptvDatabase::class.java,
        "iptv_cinema.db",
    ).addMigrations(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
    )
        // Room picks TRUNCATE on low-RAM devices (like our target TVs), where UI reads wait
        // behind sync writes. WAL lets reads run while a sync is writing.
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .build()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE movies ADD COLUMN addedAt INTEGER")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_programs_window " +
                    "ON programs(sourceId, channelId, startEpochMs)",
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE movies ADD COLUMN cast TEXT")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE series ADD COLUMN cast TEXT")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE movies ADD COLUMN youtubeTrailer TEXT")
            db.execSQL("ALTER TABLE series ADD COLUMN youtubeTrailer TEXT")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS cached_favorites (
                    id TEXT NOT NULL PRIMARY KEY,
                    profileId TEXT NOT NULL,
                    sourceId TEXT,
                    contentId TEXT NOT NULL,
                    contentType TEXT NOT NULL,
                    title TEXT NOT NULL,
                    posterUrl TEXT,
                    createdAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_cached_favorites_profileId ON cached_favorites(profileId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS cached_watch_history (
                    id TEXT NOT NULL PRIMARY KEY,
                    profileId TEXT NOT NULL,
                    sourceId TEXT,
                    contentId TEXT NOT NULL,
                    contentType TEXT NOT NULL,
                    seriesId TEXT,
                    title TEXT NOT NULL,
                    posterUrl TEXT,
                    positionMs INTEGER NOT NULL,
                    durationMs INTEGER,
                    lastWatchedAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_cached_watch_history_profileId " +
                    "ON cached_watch_history(profileId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_cached_watch_history_profileId_lastWatchedAtEpochMs " +
                    "ON cached_watch_history(profileId, lastWatchedAtEpochMs)",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS cached_user_settings (
                    userId TEXT NOT NULL PRIMARY KEY,
                    defaultAudioLanguage TEXT NOT NULL,
                    defaultSubtitleLanguage TEXT,
                    subtitlesEnabled INTEGER NOT NULL,
                    autoplayNextEpisode INTEGER NOT NULL,
                    continueWatchingEnabled INTEGER NOT NULL,
                    skipIntroEnabled INTEGER NOT NULL,
                    streamingQuality TEXT NOT NULL,
                    theme TEXT NOT NULL
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS cached_parental_controls (
                    id TEXT NOT NULL,
                    userId TEXT NOT NULL,
                    profileId TEXT NOT NULL PRIMARY KEY,
                    pinHash TEXT,
                    hideAdultCategories INTEGER NOT NULL,
                    lockPlaylistSettings INTEGER NOT NULL,
                    lockLiveCategories INTEGER NOT NULL,
                    maxRating TEXT,
                    blockedCategoriesJson TEXT NOT NULL
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS cached_playlist_sources (
                    id TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    serverUrl TEXT,
                    playlistUrl TEXT,
                    epgUrl TEXT,
                    isActive INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    lastSyncedAtEpochMs INTEGER,
                    cachedAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_cached_playlist_sources_userId " +
                    "ON cached_playlist_sources(userId)",
            )
        }
    }

    internal val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS catalog_sync_metadata (
                    sourceId TEXT NOT NULL,
                    resourceKey TEXT NOT NULL,
                    etag TEXT,
                    lastModified TEXT,
                    contentFingerprint TEXT,
                    lastCheckedAtEpochMs INTEGER NOT NULL,
                    PRIMARY KEY(sourceId, resourceKey)
                )
                """.trimIndent(),
            )
        }
    }

    internal val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE channels ADD COLUMN archiveDays INTEGER NOT NULL DEFAULT 0")
            // Force the next sync to refetch live streams so catch-up info is filled in
            // even when the provider would otherwise answer 304 Not Modified.
            db.execSQL("DELETE FROM catalog_sync_metadata WHERE resourceKey = 'xtream_live_streams'")
        }
    }

    // Each index matches a query shape in CatalogDaos, since the (id, sourceId) primary keys
    // cannot serve "WHERE sourceId = ?". Names and columns must match the @Entity indices
    // exactly, or Room's schema check fails after the migration.
    internal val CATALOG_INDEXES_V10 = listOf(
        "CREATE INDEX IF NOT EXISTS `idx_categories_type_order` " +
            "ON `categories` (`sourceId`, `contentType`, `sortOrder`, `name`)",
        "CREATE INDEX IF NOT EXISTS `idx_channels_order` ON `channels` (`sourceId`, `sortOrder`)",
        "CREATE INDEX IF NOT EXISTS `idx_channels_category` ON `channels` (`sourceId`, `categoryId`, `sortOrder`)",
        "CREATE INDEX IF NOT EXISTS `idx_movies_order` ON `movies` (`sourceId`, `sortOrder`)",
        "CREATE INDEX IF NOT EXISTS `idx_movies_category` ON `movies` (`sourceId`, `categoryId`, `sortOrder`)",
        "CREATE INDEX IF NOT EXISTS `idx_movies_category_name` " +
            "ON `movies` (`sourceId`, `categoryName`, `addedAt`, `sortOrder`)",
        "CREATE INDEX IF NOT EXISTS `idx_movies_added` ON `movies` (`sourceId`, `addedAt`, `sortOrder`)",
        "CREATE INDEX IF NOT EXISTS `idx_movies_rating` ON `movies` (`sourceId`, `ratingValue`, `year`)",
        "CREATE INDEX IF NOT EXISTS `idx_series_order` ON `series` (`sourceId`, `sortOrder`)",
        "CREATE INDEX IF NOT EXISTS `idx_series_category` ON `series` (`sourceId`, `categoryId`, `sortOrder`)",
        "CREATE INDEX IF NOT EXISTS `idx_series_category_name` " +
            "ON `series` (`sourceId`, `categoryName`, `year`, `sortOrder`)",
        "CREATE INDEX IF NOT EXISTS `idx_series_year` ON `series` (`sourceId`, `year`, `sortOrder`)",
        "CREATE INDEX IF NOT EXISTS `idx_series_rating` ON `series` (`sourceId`, `ratingValue`, `year`)",
        "CREATE INDEX IF NOT EXISTS `idx_episodes_series` " +
            "ON `episodes` (`sourceId`, `seriesId`, `seasonNumber`, `episodeNumber`)",
    )

    /** Adds the catalog indexes and the numeric rating column the top-rated rails sort on. */
    internal val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE movies ADD COLUMN ratingValue REAL")
            db.execSQL("ALTER TABLE series ADD COLUMN ratingValue REAL")
            // Same value RatingValue.of() gives new rows. Filled before the indexes exist so each
            // index is built once instead of updated row by row.
            db.execSQL("UPDATE movies SET ratingValue = CAST(rating AS REAL) WHERE rating IS NOT NULL")
            db.execSQL("UPDATE series SET ratingValue = CAST(rating AS REAL) WHERE rating IS NOT NULL")
            CATALOG_INDEXES_V10.forEach(db::execSQL)
        }
    }

    internal const val MOVIES_HERO_INDEX_V11 =
        "CREATE INDEX IF NOT EXISTS `idx_movies_hero` ON `movies` (`sourceId`, `hasHeroArt`, `addedAt`, `sortOrder`)"

    /** Adds the hero-art flag so the Home hero query reads an index instead of every movie. */
    internal val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE movies ADD COLUMN hasHeroArt INTEGER NOT NULL DEFAULT 0")
            // Same value HeroArt.of() gives new rows; filled before the index exists.
            db.execSQL(
                "UPDATE movies SET hasHeroArt = 1 " +
                    "WHERE backdropUrl IS NOT NULL AND TRIM(backdropUrl) != '' " +
                    "AND plot IS NOT NULL AND TRIM(plot) != ''",
            )
            db.execSQL(MOVIES_HERO_INDEX_V11)
        }
    }

    @Provides
    fun provideUserDataCacheDao(database: IptvDatabase): UserDataCacheDao = database.userDataCacheDao()
}
