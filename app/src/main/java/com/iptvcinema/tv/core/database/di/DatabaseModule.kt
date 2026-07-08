package com.iptvcinema.tv.core.database.di

import android.content.Context
import androidx.room.Room
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
    ).build()

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

    @Provides
    fun provideUserDataCacheDao(database: IptvDatabase): UserDataCacheDao = database.userDataCacheDao()
}
