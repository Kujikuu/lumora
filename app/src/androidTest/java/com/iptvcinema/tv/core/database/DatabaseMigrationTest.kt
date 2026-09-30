package com.iptvcinema.tv.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iptvcinema.tv.core.database.di.DatabaseModule
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
}
