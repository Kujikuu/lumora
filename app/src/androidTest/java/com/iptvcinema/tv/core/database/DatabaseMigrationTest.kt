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
