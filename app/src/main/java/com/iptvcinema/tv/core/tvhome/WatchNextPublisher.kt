package com.iptvcinema.tv.core.tvhome

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.BaseColumns
import android.util.Log
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import com.iptvcinema.tv.core.util.safeSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Writes the app's Watch Next row on the Android TV home screen. Every call is best effort:
 * devices without the TV provider, or providers that refuse a write, are logged and skipped.
 */
@Singleton
class WatchNextPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private val helper by lazy { PreviewChannelHelper(context) }

    private val isSupported: Boolean by lazy {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    suspend fun publish(entries: List<WatchNextEntry>) = runProviderWork("publish") {
        val plan = WatchNextDiff.plan(queryOwnRows().map { it.id to it.key }, entries)
        plan.deletes.forEach(::deleteRow)
        plan.updates.forEach { (rowId, entry) -> helper.updateWatchNextProgram(entry.toProgram(), rowId) }
        plan.inserts.forEach { entry -> helper.publishWatchNextProgram(entry.toProgram()) }
    }

    suspend fun clear() = runProviderWork("clear") {
        queryOwnRows().forEach { deleteRow(it.id) }
    }

    /** The key of one of the app's rows, used when the viewer removes it from the home screen. */
    suspend fun keyForRow(rowId: Long): String? = withContext(Dispatchers.IO) {
        if (!isSupported) return@withContext null
        runCatching { queryOwnRows().firstOrNull { it.id == rowId }?.key }.getOrNull()
    }

    private suspend fun runProviderWork(label: String, block: () -> Unit) {
        if (!isSupported) return
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    block()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.w(TAG, "Watch Next $label failed: ${error.safeSummary()}")
                }
            }
        }
    }

    private data class Row(val id: Long, val key: String?)

    @SuppressLint("RestrictedApi")
    private fun queryOwnRows(): List<Row> {
        // The provider only returns rows this package inserted.
        val cursor = context.contentResolver.query(
            TvContractCompat.WatchNextPrograms.CONTENT_URI,
            arrayOf(BaseColumns._ID, TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID),
            null,
            null,
            null,
        ) ?: return emptyList()
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(Row(id = it.getLong(0), key = if (it.isNull(1)) null else it.getString(1)))
                }
            }
        }
    }

    private fun deleteRow(rowId: Long) {
        context.contentResolver.delete(TvContractCompat.buildWatchNextProgramUri(rowId), null, null)
    }

    @SuppressLint("RestrictedApi")
    private fun WatchNextEntry.toProgram(): WatchNextProgram {
        val builder = WatchNextProgram.Builder()
            .setInternalProviderId(key)
            .setType(
                when (programKind) {
                    WatchNextProgramKind.Movie -> TvContractCompat.WatchNextPrograms.TYPE_MOVIE
                    WatchNextProgramKind.Episode -> TvContractCompat.WatchNextPrograms.TYPE_TV_EPISODE
                },
            )
            .setWatchNextType(
                when (kind) {
                    WatchNextKind.Continue -> TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE
                    WatchNextKind.Next -> TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_NEXT
                },
            )
            .setLastEngagementTimeUtcMillis(lastEngagementMs)
            .setTitle(title)
            .setIntentUri(Uri.parse(intentUri))
            .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9)
        posterUrl?.let { builder.setPosterArtUri(Uri.parse(it)) }
        episodeTitle?.let(builder::setEpisodeTitle)
        seasonNumber?.let { builder.setSeasonNumber(it) }
        episodeNumber?.let { builder.setEpisodeNumber(it) }
        durationMs?.let { builder.setDurationMillis(it.toIntMillis()) }
        if (kind == WatchNextKind.Continue && positionMs > 0L) {
            builder.setLastPlaybackPositionMillis(positionMs.toIntMillis())
        }
        return builder.build()
    }

    private fun Long.toIntMillis(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    private companion object {
        const val TAG = "WatchNext"
    }
}
