package com.iptvcinema.tv.core.tvhome.launcher

import com.iptvcinema.tv.core.tvhome.HomeChannelsSync
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets the content provider reach the Hilt graph (providers cannot be @AndroidEntryPoint). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LauncherEntryPoint {
    fun homeChannelsSync(): HomeChannelsSync
}
