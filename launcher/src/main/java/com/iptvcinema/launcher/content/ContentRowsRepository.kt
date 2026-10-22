package com.iptvcinema.launcher.content

import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The rows Lumora Play publishes. The launcher's own copy on disk is read synchronously at
 * construction (a few KB) so the first frame already shows rows; the provider is asked after that
 * and whenever it reports a change.
 */
class ContentRowsRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val cacheFile = File(context.filesDir, CACHE_FILE)
    private val rowsUri = Uri.parse(LauncherRowsContract.CONTENT_URI)
    private val _state = MutableStateFlow(ContentState(snapshot = readCache()))
    val state: StateFlow<ContentState> = _state.asStateFlow()

    @Volatile
    private var lastAppRefreshRequestMs = 0L
    private var observer: ContentObserver? = null

    /** Watches the provider; the first query runs as soon as the caller is ready for it. */
    fun start() {
        if (observer != null) return
        val thread = HandlerThread("launcher-rows").apply { start() }
        observer = object : ContentObserver(Handler(thread.looper)) {
            override fun onChange(selfChange: Boolean) {
                refresh()
            }
        }.also { runCatching { context.contentResolver.registerContentObserver(rowsUri, false, it) } }
        refresh()
    }

    /** Re-reads the provider; the state only changes when the rows did. */
    fun refresh() {
        scope.launch {
            val fresh = try {
                withContext(Dispatchers.IO) { query() }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Rows query failed: ${error.message}")
                null
            }
            val installed = isAppInstalled()
            val changed = fresh != null && fresh != _state.value.snapshot
            _state.update { current ->
                val snapshot = fresh ?: if (installed) current.snapshot else null
                ContentState(appInstalled = installed, snapshot = snapshot)
            }
            withContext(Dispatchers.IO) {
                if (changed && fresh != null) writeCache(fresh)
                if (!installed) cacheFile.delete()
            }
        }
    }

    /** Asks the app to rebuild its rows (Live now goes stale), at most once per [APP_REFRESH_MIN_GAP_MS]. */
    fun requestAppRefresh(nowMs: Long = System.currentTimeMillis()) {
        if (nowMs - lastAppRefreshRequestMs < APP_REFRESH_MIN_GAP_MS) return
        lastAppRefreshRequestMs = nowMs
        scope.launch(Dispatchers.IO) {
            runCatching { context.contentResolver.call(rowsUri, LauncherRowsContract.METHOD_REFRESH, null, null) }
                .onFailure { Log.w(TAG, "Refresh request failed: ${it.message}") }
        }
    }

    private fun query(): ContentSnapshot? {
        val cursor = context.contentResolver.query(rowsUri, null, null, null, null) ?: return null
        return cursor.use { ProviderRowsParser.parse(it.toMaps()) }
    }

    private fun Cursor.toMaps(): List<Map<String, Any?>> = buildList {
        val names = columnNames
        while (moveToNext()) {
            add(
                names.indices.associate { index ->
                    names[index] to when (getType(index)) {
                        Cursor.FIELD_TYPE_INTEGER -> getLong(index)
                        Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
                        Cursor.FIELD_TYPE_STRING -> getString(index)
                        else -> null
                    }
                },
            )
        }
    }

    private fun isAppInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(LauncherRowsContract.APP_PACKAGE, 0)
        true
    } catch (error: PackageManager.NameNotFoundException) {
        false
    }

    private fun readCache(): ContentSnapshot? =
        runCatching { if (cacheFile.isFile) ContentRowsCodec.decode(cacheFile.readText()) else null }.getOrNull()

    private fun writeCache(snapshot: ContentSnapshot) {
        runCatching {
            val temp = File(context.filesDir, "$CACHE_FILE.tmp")
            temp.writeText(ContentRowsCodec.encode(snapshot))
            if (!temp.renameTo(cacheFile)) {
                cacheFile.writeText(temp.readText())
                temp.delete()
            }
        }.onFailure { Log.w(TAG, "Rows cache write failed: ${it.message}") }
    }

    private companion object {
        const val TAG = "LauncherRows"
        const val CACHE_FILE = "rows.json"
        const val APP_REFRESH_MIN_GAP_MS = 10 * 60 * 1000L
    }
}
