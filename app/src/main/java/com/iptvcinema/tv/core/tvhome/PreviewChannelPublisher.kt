package com.iptvcinema.tv.core.tvhome

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.BaseColumns
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.iptvcinema.tv.core.datastore.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.iptvcinema.tv.R
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
 * Writes the app's own rows ("channels") on the Android TV home screen. Best effort like
 * [WatchNextPublisher]: a missing provider or a refused write is logged and skipped.
 */
@Singleton
class PreviewChannelPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    private val mutex = Mutex()
    private val helper by lazy { PreviewChannelHelper(context) }

    private val isSupported: Boolean by lazy {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    /** Makes the home screen hold exactly [specs], in order, and nothing else of ours. */
    suspend fun publish(specs: List<HomeChannelSpec>) = runProviderWork("publish") {
        val ownChannels = queryOwnChannels()
        val byKey = ownChannels.associateBy { it.key }
        val wantedKeys = specs.map { fullKey(it.key) }.toSet()
        ownChannels.filter { it.key !in wantedKeys }.forEach { helper.deletePreviewChannel(it.id) }
        specs.forEachIndexed { index, spec ->
            val existing = byKey[fullKey(spec.key)]
            val channelId = existing?.id ?: helper.publishChannel(buildChannel(spec))
            if (existing != null && existing.displayName != spec.displayName) {
                helper.updatePreviewChannel(channelId, buildChannel(spec))
            }
            syncPrograms(channelId, spec.programs)
            // Only the first row may ask to be shown without the viewer visiting the launcher's
            // settings, and the system question is asked once, not on every sync.
            if (index == 0 && existing?.browsable != true) requestBrowsableOnce(channelId)
        }
        Log.i(TAG, "Home rows: " + queryOwnChannels().joinToString { "${it.key} browsable=${it.browsable}" })
    }

    private suspend fun requestBrowsableOnce(channelId: Long) {
        val asked = dataStore.data.map { it[AppPreferences.HOME_CHANNEL_BROWSABLE_ASKED] ?: false }.first()
        if (asked) return
        dataStore.edit { it[AppPreferences.HOME_CHANNEL_BROWSABLE_ASKED] = true }
        TvContractCompat.requestChannelBrowsable(context, channelId)
    }

    suspend fun clear() = runProviderWork("clear") {
        queryOwnChannels().forEach { helper.deletePreviewChannel(it.id) }
    }

    private data class ChannelRow(val id: Long, val key: String, val displayName: String?, val browsable: Boolean)

    // PreviewChannelHelper.getAllChannels() throws on a channel row without a description, so
    // the app's channels are read with a plain query instead.
    @SuppressLint("RestrictedApi")
    private fun queryOwnChannels(): List<ChannelRow> {
        val cursor = context.contentResolver.query(
            TvContractCompat.Channels.CONTENT_URI,
            arrayOf(
                BaseColumns._ID,
                TvContractCompat.Channels.COLUMN_INTERNAL_PROVIDER_ID,
                TvContractCompat.Channels.COLUMN_DISPLAY_NAME,
                TvContractCompat.Channels.COLUMN_BROWSABLE,
            ),
            null,
            null,
            null,
        ) ?: return emptyList()
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    val key = if (it.isNull(1)) null else it.getString(1)
                    if (key == null || !key.startsWith(KEY_PREFIX)) continue
                    val name = if (it.isNull(2)) null else it.getString(2)
                    val browsable = !it.isNull(3) && it.getInt(3) == 1
                    add(ChannelRow(it.getLong(0), key, name, browsable))
                }
            }
        }
    }

    private fun buildChannel(spec: HomeChannelSpec): PreviewChannel {
        val builder = PreviewChannel.Builder()
            .setDisplayName(spec.displayName)
            .setDescription(spec.displayName)
            .setInternalProviderId(fullKey(spec.key))
        // The provider dereferences the app link, so a channel must always carry one.
        builder.setAppLinkIntent(appLaunchIntent())
        AppCompatResources.getDrawable(context, R.drawable.tv_banner)
            ?.toBitmap(LOGO_SIZE_PX, LOGO_SIZE_PX)
            ?.let(builder::setLogo)
        return builder.build()
    }

    private fun appLaunchIntent(): Intent {
        val packageManager = context.packageManager
        return packageManager.getLeanbackLaunchIntentForPackage(context.packageName)
            ?: packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setClassName(context, MAIN_ACTIVITY)
    }

    private fun syncPrograms(channelId: Long, programs: List<HomeProgramEntry>) {
        val existing = queryPrograms(channelId)
        val plan = ProviderRowDiff.plan(existing, programs) { it.key }
        plan.deletes.forEach(helper::deletePreviewProgram)
        plan.updates.forEach { (rowId, entry) ->
            helper.updatePreviewProgram(rowId, entry.toProgram(channelId, programs.indexOf(entry)))
        }
        plan.inserts.forEach { entry ->
            helper.publishPreviewProgram(entry.toProgram(channelId, programs.indexOf(entry)))
        }
    }

    @SuppressLint("RestrictedApi")
    private fun queryPrograms(channelId: Long): List<Pair<Long, String?>> {
        val cursor = context.contentResolver.query(
            TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
            arrayOf(BaseColumns._ID, TvContractCompat.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID),
            null,
            null,
            null,
        ) ?: return emptyList()
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(it.getLong(0) to (if (it.isNull(1)) null else it.getString(1)))
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun HomeProgramEntry.toProgram(channelId: Long, position: Int): PreviewProgram {
        val builder = PreviewProgram.Builder()
            .setChannelId(channelId)
            .setInternalProviderId(key)
            .setType(
                when (kind) {
                    HomeProgramKind.Movie -> TvContractCompat.PreviewPrograms.TYPE_MOVIE
                    HomeProgramKind.Series -> TvContractCompat.PreviewPrograms.TYPE_TV_SERIES
                    HomeProgramKind.Channel -> TvContractCompat.PreviewPrograms.TYPE_CHANNEL
                },
            )
            .setTitle(title)
            .setIntentUri(Uri.parse(intentUri))
            .setPosterArtAspectRatio(
                if (isWidePoster) {
                    TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9
                } else {
                    TvContractCompat.PreviewPrograms.ASPECT_RATIO_2_3
                },
            )
            // Higher weight sorts first; keep the row in the order it was built.
            .setWeight(WEIGHT_BASE - position)
        description?.takeIf { it.isNotBlank() }?.let(builder::setDescription)
        posterUrl?.takeIf { it.isNotBlank() }?.let { builder.setPosterArtUri(Uri.parse(it)) }
        return builder.build()
    }

    private suspend fun runProviderWork(label: String, block: suspend () -> Unit) {
        if (!isSupported) return
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    block()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.w(TAG, "Home channel $label failed: ${error.safeSummary()}", error)
                }
            }
        }
    }

    private fun fullKey(key: String) = KEY_PREFIX + key

    private companion object {
        const val TAG = "HomeChannels"
        const val KEY_PREFIX = "lumora:"
        const val LOGO_SIZE_PX = 160
        const val WEIGHT_BASE = 1_000
        const val MAIN_ACTIVITY = "com.iptvcinema.tv.app.MainActivity"
    }
}
