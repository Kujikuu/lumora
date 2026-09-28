package com.iptvcinema.tv.core.data.repository.di

import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.DeviceActivationRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository
import com.iptvcinema.tv.core.data.repository.ProfilesRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.data.repository.RoutingFavoritesRepository
import com.iptvcinema.tv.core.data.repository.RoutingParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.RoutingProfilesRepository
import com.iptvcinema.tv.core.data.repository.RoutingUserSettingsRepository
import com.iptvcinema.tv.core.data.repository.RoutingWatchHistoryRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseAuthRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseDeviceActivationRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabasePlaylistSourcesRepository
import com.iptvcinema.tv.core.datastore.SessionPreparer
import com.iptvcinema.tv.core.datastore.StartupSessionBootstrap
import com.iptvcinema.tv.core.device.AndroidDeviceIdentity
import com.iptvcinema.tv.core.device.DeviceIdentity
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: SupabaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDeviceActivationRepository(impl: SupabaseDeviceActivationRepository): DeviceActivationRepository

    @Binds
    @Singleton
    abstract fun bindProfilesRepository(impl: RoutingProfilesRepository): ProfilesRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistSourcesRepository(impl: SupabasePlaylistSourcesRepository): PlaylistSourcesRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: RoutingFavoritesRepository): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindWatchHistoryRepository(impl: RoutingWatchHistoryRepository): WatchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindUserSettingsRepository(impl: RoutingUserSettingsRepository): UserSettingsRepository

    @Binds
    @Singleton
    abstract fun bindParentalControlsRepository(impl: RoutingParentalControlsRepository): ParentalControlsRepository

    @Binds
    abstract fun bindSessionPreparer(impl: StartupSessionBootstrap): SessionPreparer

    @Binds
    abstract fun bindDeviceIdentity(impl: AndroidDeviceIdentity): DeviceIdentity
}
