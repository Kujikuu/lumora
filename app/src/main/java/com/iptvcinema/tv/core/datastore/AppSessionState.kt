package com.iptvcinema.tv.core.datastore

import com.iptvcinema.tv.core.model.SourceType

data class AppSessionState(
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val hasSource: Boolean = false,
    val currentProfileId: String? = null,
    val currentSourceId: String? = null,
    val sourceType: SourceType? = null,
    val isDemoMode: Boolean = false,
) {
    fun resolveStartupDestination(): StartupDestination = when {
        !isAuthenticated -> StartupDestination.Welcome
        !hasSource -> StartupDestination.AddSource
        currentProfileId == null -> StartupDestination.ProfileSelection
        else -> StartupDestination.Home
    }

    fun meetsRequirement(requirement: SessionRequirement): Boolean = when (requirement) {
        SessionRequirement.None -> true
        SessionRequirement.Authenticated -> isAuthenticated
        SessionRequirement.HasSource -> isAuthenticated && hasSource
        SessionRequirement.Ready -> isAuthenticated && hasSource && currentProfileId != null
    }

    fun redirectRouteFor(requirement: SessionRequirement): String? {
        if (meetsRequirement(requirement)) return null
        return resolveStartupDestination().route()
    }
}

enum class SessionRequirement {
    None,
    Authenticated,
    HasSource,
    Ready,
}

fun AppSessionState.connectedSourceLabel(): String? = when {
    !hasSource -> null
    isDemoMode -> "Demo Mode"
    sourceType == SourceType.DEMO -> "Demo Mode"
    sourceType == SourceType.XTREAM_CODES -> "Xtream Codes"
    sourceType == SourceType.M3U -> "M3U Playlist"
    else -> currentSourceId
}

sealed class StartupDestination {
    data object Welcome : StartupDestination()
    data object Activation : StartupDestination()
    data object AddSource : StartupDestination()
    data object ProfileSelection : StartupDestination()
    data object Home : StartupDestination()
}

fun StartupDestination.route(): String = when (this) {
    StartupDestination.Welcome -> com.iptvcinema.tv.core.navigation.AppRoute.WELCOME
    StartupDestination.Activation -> com.iptvcinema.tv.core.navigation.AppRoute.ACTIVATION
    StartupDestination.AddSource -> com.iptvcinema.tv.core.navigation.AppRoute.ADD_SOURCE
    StartupDestination.ProfileSelection -> com.iptvcinema.tv.core.navigation.AppRoute.PROFILE_SELECTION
    StartupDestination.Home -> com.iptvcinema.tv.core.navigation.AppRoute.HOME
}
