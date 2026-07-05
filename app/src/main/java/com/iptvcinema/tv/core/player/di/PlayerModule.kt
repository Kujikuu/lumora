package com.iptvcinema.tv.core.player.di

import com.iptvcinema.tv.core.player.ContinueWatchingCatalog
import com.iptvcinema.tv.core.player.DefaultContinueWatchingCatalog
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {
    @Binds
    @Singleton
    abstract fun bindContinueWatchingCatalog(
        impl: DefaultContinueWatchingCatalog,
    ): ContinueWatchingCatalog
}
