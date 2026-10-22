package com.iptvcinema.tv.core.reminders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A "remind me" set on an upcoming programme in the guide. */
@Serializable
data class ProgramReminder(
    @SerialName("id") val id: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("channel_name") val channelName: String,
    @SerialName("program_id") val programId: String,
    @SerialName("title") val title: String,
    @SerialName("start_ms") val startEpochMs: Long,
    @SerialName("end_ms") val endEpochMs: Long,
) {
    companion object {
        fun idFor(profileId: String, channelId: String, programId: String): String =
            "$profileId:$channelId:$programId"
    }
}
