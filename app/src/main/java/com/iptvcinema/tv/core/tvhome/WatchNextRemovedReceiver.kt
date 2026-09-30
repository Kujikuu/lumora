package com.iptvcinema.tv.core.tvhome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.tvprovider.media.tv.TvContractCompat
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.datastore.AppSessionRepository
import com.iptvcinema.tv.core.di.ApplicationScope
import com.iptvcinema.tv.core.model.WatchHistoryContentType
import com.iptvcinema.tv.core.util.safeSummary
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The viewer removed one of our cards from the home screen's Watch Next row. Drop it from
 * Continue Watching too, so the app and the home screen agree and the card does not come back.
 */
@AndroidEntryPoint
class WatchNextRemovedReceiver : BroadcastReceiver() {
    @Inject lateinit var publisher: WatchNextPublisher
    @Inject lateinit var watchHistoryRepository: WatchHistoryRepository
    @Inject lateinit var appSessionRepository: AppSessionRepository
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TvContractCompat.ACTION_WATCH_NEXT_PROGRAM_BROWSABLE_DISABLED) return
        val rowId = intent.getLongExtra(TvContractCompat.EXTRA_WATCH_NEXT_PROGRAM_ID, -1L)
        if (rowId < 0L) return
        val pending = goAsync()
        applicationScope.launch {
            try {
                val key = publisher.keyForRow(rowId) ?: return@launch
                val profileId = appSessionRepository.sessionState.first().currentProfileId ?: return@launch
                val removed = WatchNextKeys.parse(key) ?: return@launch
                when (removed) {
                    is WatchNextKeys.Removed.Series -> watchHistoryRepository.removeSeries(profileId, removed.seriesId)
                    is WatchNextKeys.Removed.Item ->
                        watchHistoryRepository.remove(profileId, removed.contentId, removed.contentType)
                }
                watchHistoryRepository.invalidate()
            } catch (error: Exception) {
                Log.w("WatchNext", "Could not remove a Watch Next item: ${error.safeSummary()}")
            } finally {
                pending.finish()
            }
        }
    }
}

/** Reads back the keys [WatchNextEntryMapper] writes. */
object WatchNextKeys {
    sealed class Removed {
        data class Series(val seriesId: String) : Removed()
        data class Item(val contentId: String, val contentType: WatchHistoryContentType) : Removed()
    }

    fun parse(key: String): Removed? {
        val type = key.substringBefore(':', missingDelimiterValue = "")
        val id = key.substringAfter(':', missingDelimiterValue = "").takeIf { it.isNotBlank() } ?: return null
        return when (type) {
            "series" -> Removed.Series(id)
            "movie" -> Removed.Item(id, WatchHistoryContentType.MOVIE)
            "episode" -> Removed.Item(id, WatchHistoryContentType.EPISODE)
            else -> null
        }
    }
}
