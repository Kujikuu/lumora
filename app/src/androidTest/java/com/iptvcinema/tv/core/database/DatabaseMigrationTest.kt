package com.iptvcinema.tv.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iptvcinema.tv.core.database.di.DatabaseModule
import com.iptvcinema.tv.core.database.entity.HeroArt
import com.iptvcinema.tv.core.database.entity.RatingValue
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @Test
    fun migration7To8PreservesExistingDataAndCreatesSyncMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-7-8-test.db"
        context.deleteDatabase(databaseName)

        val version7 = openHelper(context, databaseName, version = 7) { database ->
            database.execSQL("CREATE TABLE legacy_catalog (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL)")
            database.execSQL("INSERT INTO legacy_catalog (id, title) VALUES ('1', 'Cached item')")
        }
        version7.writableDatabase
        version7.close()

        val version8 = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(8) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                            DatabaseModule.MIGRATION_7_8.migrate(db)
                        }
                    },
                )
                .build(),
        )

        try {
            val migrated = version8.writableDatabase
            migrated.query("SELECT title FROM legacy_catalog WHERE id = '1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Cached item", cursor.getString(0))
            }
            migrated.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'catalog_sync_metadata'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        } finally {
            version8.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun migration8To9AddsArchiveDaysAndForcesLiveStreamRefetch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-8-9-test.db"
        context.deleteDatabase(databaseName)

        val version8 = openHelper(context, databaseName, version = 8) { database ->
            database.execSQL(
                "CREATE TABLE channels (id TEXT NOT NULL, sourceId TEXT NOT NULL, name TEXT NOT NULL, " +
                    "streamUrl TEXT NOT NULL, logoUrl TEXT, categoryId TEXT, categoryName TEXT, tvgId TEXT, " +
                    "channelNumber INTEGER, isAdult INTEGER NOT NULL, sortOrder INTEGER NOT NULL, " +
                    "PRIMARY KEY(id, sourceId))",
            )
            database.execSQL(
                "INSERT INTO channels (id, sourceId, name, streamUrl, isAdult, sortOrder) " +
                    "VALUES ('101', 'src', 'News', 'http://x/live/u/p/101.ts', 0, 0)",
            )
            DatabaseModule.MIGRATION_7_8.migrate(database)
            database.execSQL(
                "INSERT INTO catalog_sync_metadata (sourceId, resourceKey, etag, lastCheckedAtEpochMs) " +
                    "VALUES ('src', 'xtream_live_streams', 'etag-1', 1), ('src', 'xtream_vod_streams', 'etag-2', 1)",
            )
        }
        version8.writableDatabase
        version8.close()

        val version9 = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(9) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                            DatabaseModule.MIGRATION_8_9.migrate(db)
                        }
                    },
                )
                .build(),
        )

        try {
            val migrated = version9.writableDatabase
            migrated.query("SELECT name, archiveDays FROM channels WHERE id = '101'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("News", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
            }
            migrated.query("SELECT resourceKey FROM catalog_sync_metadata").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("xtream_vod_streams", cursor.getString(0))
            }
        } finally {
            version9.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun migration9To10BackfillsRatingValueAndCreatesCatalogIndexes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-9-10-test.db"
        context.deleteDatabase(databaseName)

        val version9 = openHelper(context, databaseName, version = 9) { database ->
            Version9Schema.statements.forEach(database::execSQL)
            insertVersion9Movies(database)
        }
        version9.writableDatabase
        version9.close()

        val version10 = upgradeHelper(context, databaseName, version = 10) { db ->
            DatabaseModule.MIGRATION_9_10.migrate(db)
        }

        try {
            val migrated = version10.writableDatabase
            migrated.query("SELECT id, title, rating, ratingValue FROM movies ORDER BY id").use { cursor ->
                assertEquals(4, cursor.count)
                assertTrue(cursor.moveToNext())
                assertEquals("Top", cursor.getString(1))
                assertEquals("7.5/10", cursor.getString(2))
                assertEquals(7.5, cursor.getDouble(3), 0.0)
                assertTrue(cursor.moveToNext())
                assertTrue(cursor.isNull(2))
                assertTrue(cursor.isNull(3))
                assertTrue(cursor.moveToNext())
                assertEquals(0.0, cursor.getDouble(3), 0.0)
                assertTrue(cursor.moveToNext())
                assertEquals(8.0, cursor.getDouble(3), 0.0)
            }
            val indexNames = CATALOG_INDEX_NAMES.joinToString(",") { "'$it'" }
            migrated.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name IN ($indexNames)",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(CATALOG_INDEX_NAMES.size, cursor.getInt(0))
            }
            migrated.query(
                "EXPLAIN QUERY PLAN SELECT * FROM movies WHERE sourceId = 'src' " +
                    "AND ratingValue >= 7 ORDER BY ratingValue DESC, year DESC LIMIT 10",
            ).use { cursor ->
                val plan = buildString { while (cursor.moveToNext()) append(cursor.getString(3)) }
                assertTrue(plan, plan.contains("idx_movies_rating"))
            }
        } finally {
            version10.close()
            context.deleteDatabase(databaseName)
        }
    }

    /** Room checks every table and index after migrating, so this fails if v11 drifts from the entities. */
    @Test
    fun migration9To11PassesRoomSchemaValidation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-9-11-room-test.db"
        context.deleteDatabase(databaseName)

        val version9 = openHelper(context, databaseName, version = 9) { database ->
            Version9Schema.statements.forEach(database::execSQL)
            insertVersion9Movies(database)
            database.execSQL("UPDATE movies SET backdropUrl = 'b4', plot = 'Plot' WHERE id = '4'")
            database.execSQL("UPDATE movies SET backdropUrl = '  ', plot = 'Plot' WHERE id = '1'")
        }
        version9.writableDatabase
        version9.close()

        val room = Room.databaseBuilder(context, IptvDatabase::class.java, databaseName)
            .addMigrations(DatabaseModule.MIGRATION_9_10, DatabaseModule.MIGRATION_10_11)
            .build()
        try {
            val topRated = room.movieDao().getTopRated("src", minRating = 7.0, limit = 10)
            assertEquals(listOf("4", "1"), topRated.map { it.id })
            assertEquals(7.5, topRated.last().ratingValue ?: -1.0, 0.0)
            // Back-filled values equal what new rows get, so the next sync sees no change.
            listOf("1", "2", "3", "4").forEach { id ->
                val stored = room.movieDao().getById("src", id)
                assertEquals(RatingValue.of(stored?.rating), stored?.ratingValue)
                assertEquals(HeroArt.of(stored?.backdropUrl, stored?.plot), stored?.hasHeroArt)
            }
            assertEquals(listOf("4"), room.movieDao().getNewestWithArtwork("src", limit = 10).map { it.id })
        } finally {
            room.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun insertVersion9Movies(database: SupportSQLiteDatabase) {
        database.execSQL(
            "INSERT INTO movies (id, sourceId, title, streamUrl, rating, year, sortOrder) VALUES " +
                "('1', 'src', 'Top', 'u1', '7.5/10', 2020, 0), " +
                "('2', 'src', 'Unrated', 'u2', NULL, 2021, 1), " +
                "('3', 'src', 'Text', 'u3', 'N/A', 2022, 2), " +
                "('4', 'src', 'Best', 'u4', '8', 2019, 3)",
        )
    }

    private fun upgradeHelper(
        context: Context,
        name: String,
        version: Int,
        migrate: (SupportSQLiteDatabase) -> Unit,
    ): SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = migrate(db)
                },
            )
            .build(),
    )

    private fun openHelper(
        context: Context,
        name: String,
        version: Int,
        onCreate: (SupportSQLiteDatabase) -> Unit,
    ): SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) = onCreate(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            )
            .build(),
    )

    private companion object {
        val CATALOG_INDEX_NAMES = listOf(
            "idx_categories_type_order",
            "idx_channels_order",
            "idx_channels_category",
            "idx_movies_order",
            "idx_movies_category",
            "idx_movies_category_name",
            "idx_movies_added",
            "idx_movies_rating",
            "idx_series_order",
            "idx_series_category",
            "idx_series_category_name",
            "idx_series_year",
            "idx_series_rating",
            "idx_episodes_series",
        )
    }
}
