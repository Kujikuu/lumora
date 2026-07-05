package com.iptvcinema.tv.core.catalog

import androidx.annotation.StringRes
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.m3u.M3uSyncProgress
import com.iptvcinema.tv.core.m3u.M3uSyncStep
import com.iptvcinema.tv.core.xtream.XtreamSyncProgress
import com.iptvcinema.tv.core.xtream.XtreamSyncStep

data class CatalogSyncProgress(
    val fraction: Float,
    val milestone: SyncMilestone,
)

enum class SyncMilestone {
    CONNECTING,
    AUTHENTICATING,
    LIVE,
    MOVIES,
    SERIES,
    COMPLETE,
    M3U_VALID,
    M3U_DOWNLOAD,
    M3U_PARSE,
    M3U_COMPLETE,
}

object CatalogSyncProgressMapper {
    private const val IN_PROGRESS_PARTIAL = 0.35f

    private val xtreamMilestoneCompleteSteps = listOf(
        XtreamSyncStep.VALIDATING_URL,
        XtreamSyncStep.AUTHENTICATING,
        XtreamSyncStep.LIVE_STREAMS,
        XtreamSyncStep.VOD_STREAMS,
        XtreamSyncStep.SERIES,
        XtreamSyncStep.COMPLETE,
    )

    private val m3uMilestoneCompleteSteps = listOf(
        M3uSyncStep.VALIDATING_URL,
        M3uSyncStep.DOWNLOADING,
        M3uSyncStep.NORMALIZING,
        M3uSyncStep.COMPLETE,
    )

    fun mapXtream(steps: List<XtreamSyncProgress>): CatalogSyncProgress {
        val milestone = currentXtreamMilestone(steps)
        val fraction = computeFraction(
            milestoneCompleteSteps = xtreamMilestoneCompleteSteps,
            steps = steps,
            stepOf = XtreamSyncProgress::step,
            isSuccess = XtreamSyncProgress::isSuccess,
        )
        return CatalogSyncProgress(fraction = fraction, milestone = milestone)
    }

    fun mapM3u(steps: List<M3uSyncProgress>): CatalogSyncProgress {
        val milestone = currentM3uMilestone(steps)
        val fraction = computeFraction(
            milestoneCompleteSteps = m3uMilestoneCompleteSteps,
            steps = steps,
            stepOf = M3uSyncProgress::step,
            isSuccess = M3uSyncProgress::isSuccess,
        )
        return CatalogSyncProgress(fraction = fraction, milestone = milestone)
    }

    fun xtreamChecklistLabel(progress: XtreamSyncProgress): String = when (progress.step) {
        XtreamSyncStep.VALIDATING_URL -> "Server reachable"
        XtreamSyncStep.AUTHENTICATING -> "Authentication"
        XtreamSyncStep.LIVE_CATEGORIES, XtreamSyncStep.LIVE_STREAMS -> "Live channels"
        XtreamSyncStep.VOD_CATEGORIES, XtreamSyncStep.VOD_STREAMS -> "Movies"
        XtreamSyncStep.SERIES_CATEGORIES, XtreamSyncStep.SERIES -> "Series"
        XtreamSyncStep.WATCHED_SERIES_EPISODES -> "Watched series episodes"
        XtreamSyncStep.EPG -> "EPG"
        XtreamSyncStep.COMPLETE -> "Sync complete"
    }

    fun m3uChecklistLabel(progress: M3uSyncProgress): String = when (progress.step) {
        M3uSyncStep.VALIDATING_URL -> "Playlist URL valid"
        M3uSyncStep.DOWNLOADING -> "Playlist downloaded"
        M3uSyncStep.PARSING -> "Channels parsed"
        M3uSyncStep.NORMALIZING -> "Channels saved"
        M3uSyncStep.EPG -> "EPG"
        M3uSyncStep.COMPLETE -> "Sync complete"
    }

    @StringRes
    fun milestoneStringRes(milestone: SyncMilestone): Int = when (milestone) {
        SyncMilestone.CONNECTING -> R.string.sync_step_connecting
        SyncMilestone.AUTHENTICATING -> R.string.sync_step_authenticating
        SyncMilestone.LIVE -> R.string.sync_step_live
        SyncMilestone.MOVIES -> R.string.sync_step_movies
        SyncMilestone.SERIES -> R.string.sync_step_series
        SyncMilestone.COMPLETE -> R.string.sync_step_complete
        SyncMilestone.M3U_VALID -> R.string.sync_step_connecting
        SyncMilestone.M3U_DOWNLOAD -> R.string.sync_step_m3u_downloading
        SyncMilestone.M3U_PARSE -> R.string.sync_step_m3u_parsing
        SyncMilestone.M3U_COMPLETE -> R.string.sync_step_complete
    }

    private fun currentXtreamMilestone(steps: List<XtreamSyncProgress>): SyncMilestone {
        val lastStep = steps.lastOrNull()?.step ?: return SyncMilestone.CONNECTING
        return xtreamMilestoneForStep(lastStep)
    }

    private fun currentM3uMilestone(steps: List<M3uSyncProgress>): SyncMilestone {
        val lastStep = steps.lastOrNull()?.step ?: return SyncMilestone.M3U_VALID
        return m3uMilestoneForStep(lastStep)
    }

    private fun xtreamMilestoneForStep(step: XtreamSyncStep): SyncMilestone = when (step) {
        XtreamSyncStep.VALIDATING_URL -> SyncMilestone.CONNECTING
        XtreamSyncStep.AUTHENTICATING -> SyncMilestone.AUTHENTICATING
        XtreamSyncStep.LIVE_CATEGORIES, XtreamSyncStep.LIVE_STREAMS -> SyncMilestone.LIVE
        XtreamSyncStep.VOD_CATEGORIES, XtreamSyncStep.VOD_STREAMS -> SyncMilestone.MOVIES
        XtreamSyncStep.SERIES_CATEGORIES,
        XtreamSyncStep.SERIES,
        XtreamSyncStep.WATCHED_SERIES_EPISODES,
        -> SyncMilestone.SERIES
        XtreamSyncStep.COMPLETE -> SyncMilestone.COMPLETE
        XtreamSyncStep.EPG -> SyncMilestone.COMPLETE
    }

    private fun m3uMilestoneForStep(step: M3uSyncStep): SyncMilestone = when (step) {
        M3uSyncStep.VALIDATING_URL -> SyncMilestone.M3U_VALID
        M3uSyncStep.DOWNLOADING -> SyncMilestone.M3U_DOWNLOAD
        M3uSyncStep.PARSING, M3uSyncStep.NORMALIZING -> SyncMilestone.M3U_PARSE
        M3uSyncStep.COMPLETE -> SyncMilestone.M3U_COMPLETE
        M3uSyncStep.EPG -> SyncMilestone.M3U_COMPLETE
    }

    private fun <T, S> computeFraction(
        milestoneCompleteSteps: List<S>,
        steps: List<T>,
        stepOf: (T) -> S,
        isSuccess: (T) -> Boolean,
    ): Float {
        if (steps.isEmpty()) return 0f
        val total = milestoneCompleteSteps.size.toFloat()
        var completed = 0
        milestoneCompleteSteps.forEachIndexed { index, milestoneStep ->
            if (steps.any { stepOf(it) == milestoneStep && isSuccess(it) }) {
                completed = index + 1
            }
        }
        val last = steps.last()
        val lastStep = stepOf(last)
        val currentIndex = milestoneCompleteSteps.indexOf(lastStep)
        val partial = when {
            currentIndex < 0 -> 0f
            isSuccess(last) -> 0f
            else -> IN_PROGRESS_PARTIAL
        }
        return ((completed.toFloat() + partial) / total).coerceIn(0f, 1f)
    }
}
