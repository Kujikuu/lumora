package com.iptvcinema.tv.core.tvhome.launcher

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.EntryPointAccessors
import java.io.File

/**
 * Serves the home rows to the Lumora Home launcher. It only reads the snapshot file that
 * [com.iptvcinema.tv.core.tvhome.HomeChannelsSync] writes, so a query never touches Room, the
 * network or the Hilt graph, and costs the launcher nothing but a small file read.
 */
class LauncherContentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (uri.path?.trimStart('/') != LauncherRowsContract.PATH_ROWS) return null
        val cursor = MatrixCursor(LauncherRowsContract.COLUMNS)
        val snapshot = snapshotStore(attachedContext()).read() ?: return cursor
        LauncherRowsCursor.rowsOf(snapshot).forEach(cursor::addRow)
        cursor.setNotificationUri(attachedContext().contentResolver, Uri.parse(LauncherRowsContract.CONTENT_URI))
        return cursor
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != LauncherRowsContract.METHOD_REFRESH) return null
        runCatching {
            EntryPointAccessors.fromApplication(attachedContext().applicationContext, LauncherEntryPoint::class.java)
                .homeChannelsSync()
                .refreshNow()
        }.onFailure { Log.w(TAG, "Launcher refresh request failed: ${it.message}") }
        return Bundle.EMPTY
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.$AUTHORITY.row"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException("read only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("read only")

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("read only")

    private fun attachedContext(): Context = checkNotNull(context) { "provider not attached" }

    companion object {
        private const val TAG = "LauncherProvider"
        private const val AUTHORITY = LauncherRowsContract.AUTHORITY

        fun snapshotStore(context: Context): LauncherSnapshotStore =
            LauncherSnapshotStore(File(context.filesDir, LauncherRowsContract.SNAPSHOT_DIR))

        fun notifyRowsChanged(context: Context) {
            context.contentResolver.notifyChange(Uri.parse(LauncherRowsContract.CONTENT_URI), null)
        }
    }
}
