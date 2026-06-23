package com.iptvcinema.tv.core.datastore;

import com.iptvcinema.tv.core.data.local.LocalCredentialsStore;
import com.iptvcinema.tv.core.data.repository.AuthRepository;
import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository;
import com.iptvcinema.tv.core.player.WatchedSeriesEpisodePrefetcher;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class StartupSessionBootstrap_Factory implements Factory<StartupSessionBootstrap> {
  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider;

  private final Provider<LocalCredentialsStore> localCredentialsStoreProvider;

  private final Provider<WatchedSeriesEpisodePrefetcher> watchedSeriesEpisodePrefetcherProvider;

  public StartupSessionBootstrap_Factory(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      Provider<WatchedSeriesEpisodePrefetcher> watchedSeriesEpisodePrefetcherProvider) {
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.playlistSourcesRepositoryProvider = playlistSourcesRepositoryProvider;
    this.localCredentialsStoreProvider = localCredentialsStoreProvider;
    this.watchedSeriesEpisodePrefetcherProvider = watchedSeriesEpisodePrefetcherProvider;
  }

  @Override
  public StartupSessionBootstrap get() {
    return newInstance(appSessionRepositoryProvider.get(), authRepositoryProvider.get(), playlistSourcesRepositoryProvider.get(), localCredentialsStoreProvider.get(), watchedSeriesEpisodePrefetcherProvider.get());
  }

  public static StartupSessionBootstrap_Factory create(
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      javax.inject.Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      javax.inject.Provider<WatchedSeriesEpisodePrefetcher> watchedSeriesEpisodePrefetcherProvider) {
    return new StartupSessionBootstrap_Factory(Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(playlistSourcesRepositoryProvider), Providers.asDaggerProvider(localCredentialsStoreProvider), Providers.asDaggerProvider(watchedSeriesEpisodePrefetcherProvider));
  }

  public static StartupSessionBootstrap_Factory create(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      Provider<WatchedSeriesEpisodePrefetcher> watchedSeriesEpisodePrefetcherProvider) {
    return new StartupSessionBootstrap_Factory(appSessionRepositoryProvider, authRepositoryProvider, playlistSourcesRepositoryProvider, localCredentialsStoreProvider, watchedSeriesEpisodePrefetcherProvider);
  }

  public static StartupSessionBootstrap newInstance(AppSessionRepository appSessionRepository,
      AuthRepository authRepository, PlaylistSourcesRepository playlistSourcesRepository,
      LocalCredentialsStore localCredentialsStore,
      WatchedSeriesEpisodePrefetcher watchedSeriesEpisodePrefetcher) {
    return new StartupSessionBootstrap(appSessionRepository, authRepository, playlistSourcesRepository, localCredentialsStore, watchedSeriesEpisodePrefetcher);
  }
}
