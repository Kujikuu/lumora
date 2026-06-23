package com.iptvcinema.tv.core.data.repository.di

import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.data.repository.DeviceActivationRepository
import com.iptvcinema.tv.core.data.repository.FavoritesRepository
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository
import com.iptvcinema.tv.core.data.repository.ProfilesRepository
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseAuthRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseDeviceActivationRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseFavoritesRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseParentalControlsRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabasePlaylistSourcesRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseProfilesRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseUserSettingsRepository
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseWatchHistoryRepository
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
    abstract fun bindProfilesRepository(impl: SupabaseProfilesRepository): ProfilesRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistSourcesRepository(impl: SupabasePlaylistSourcesRepository): PlaylistSourcesRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: SupabaseFavoritesRepository): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindWatchHistoryRepository(impl: SupabaseWatchHistoryRepository): WatchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindUserSettingsRepository(impl: SupabaseUserSettingsRepository): UserSettingsRepository

    @Binds
    @Singleton
    abstract fun bindParentalControlsRepository(impl: SupabaseParentalControlsRepository): ParentalControlsRepository
}
