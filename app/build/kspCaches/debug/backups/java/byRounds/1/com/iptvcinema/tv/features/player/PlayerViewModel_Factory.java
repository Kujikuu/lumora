package com.iptvcinema.tv.features.player;

import androidx.lifecycle.SavedStateHandle;
import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository;
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import com.iptvcinema.tv.core.player.EpisodeCatalogRepository;
import com.iptvcinema.tv.core.player.NextEpisodeResolver;
import com.iptvcinema.tv.core.player.PlaybackRepository;
import com.iptvcinema.tv.core.player.PlaybackSessionTracker;
import com.iptvcinema.tv.core.player.PlayerManager;
import com.iptvcinema.tv.core.util.AppStrings;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata
@QualifierMetadata("com.iptvcinema.tv.core.di.ApplicationScope")
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
public final class PlayerViewModel_Factory implements Factory<PlayerViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<PlaybackRepository> playbackRepositoryProvider;

  private final Provider<PlayerManager> playerManagerProvider;

  private final Provider<WatchHistoryRepository> watchHistoryRepositoryProvider;

  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<UserSettingsRepository> userSettingsRepositoryProvider;

  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<NextEpisodeResolver> nextEpisodeResolverProvider;

  private final Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider;

  private final Provider<PlaybackSessionTracker> playbackSessionTrackerProvider;

  private final Provider<AppStrings> appStringsProvider;

  private final Provider<CoroutineScope> applicationScopeProvider;

  public PlayerViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<PlaybackRepository> playbackRepositoryProvider,
      Provider<PlayerManager> playerManagerProvider,
      Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<NextEpisodeResolver> nextEpisodeResolverProvider,
      Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider,
      Provider<PlaybackSessionTracker> playbackSessionTrackerProvider,
      Provider<AppStrings> appStringsProvider, Provider<CoroutineScope> applicationScopeProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.playbackRepositoryProvider = playbackRepositoryProvider;
    this.playerManagerProvider = playerManagerProvider;
    this.watchHistoryRepositoryProvider = watchHistoryRepositoryProvider;
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.userSettingsRepositoryProvider = userSettingsRepositoryProvider;
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.nextEpisodeResolverProvider = nextEpisodeResolverProvider;
    this.episodeCatalogRepositoryProvider = episodeCatalogRepositoryProvider;
    this.playbackSessionTrackerProvider = playbackSessionTrackerProvider;
    this.appStringsProvider = appStringsProvider;
    this.applicationScopeProvider = applicationScopeProvider;
  }

  @Override
  public PlayerViewModel get() {
    return newInstance(savedStateHandleProvider.get(), playbackRepositoryProvider.get(), playerManagerProvider.get(), watchHistoryRepositoryProvider.get(), appSessionRepositoryProvider.get(), userSettingsRepositoryProvider.get(), catalogRepositoryProvider.get(), nextEpisodeResolverProvider.get(), episodeCatalogRepositoryProvider.get(), playbackSessionTrackerProvider.get(), appStringsProvider.get(), applicationScopeProvider.get());
  }

  public static PlayerViewModel_Factory create(
      javax.inject.Provider<SavedStateHandle> savedStateHandleProvider,
      javax.inject.Provider<PlaybackRepository> playbackRepositoryProvider,
      javax.inject.Provider<PlayerManager> playerManagerProvider,
      javax.inject.Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<NextEpisodeResolver> nextEpisodeResolverProvider,
      javax.inject.Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider,
      javax.inject.Provider<PlaybackSessionTracker> playbackSessionTrackerProvider,
      javax.inject.Provider<AppStrings> appStringsProvider,
      javax.inject.Provider<CoroutineScope> applicationScopeProvider) {
    return new PlayerViewModel_Factory(Providers.asDaggerProvider(savedStateHandleProvider), Providers.asDaggerProvider(playbackRepositoryProvider), Providers.asDaggerProvider(playerManagerProvider), Providers.asDaggerProvider(watchHistoryRepositoryProvider), Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(userSettingsRepositoryProvider), Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(nextEpisodeResolverProvider), Providers.asDaggerProvider(episodeCatalogRepositoryProvider), Providers.asDaggerProvider(playbackSessionTrackerProvider), Providers.asDaggerProvider(appStringsProvider), Providers.asDaggerProvider(applicationScopeProvider));
  }

  public static PlayerViewModel_Factory create(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<PlaybackRepository> playbackRepositoryProvider,
      Provider<PlayerManager> playerManagerProvider,
      Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<NextEpisodeResolver> nextEpisodeResolverProvider,
      Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider,
      Provider<PlaybackSessionTracker> playbackSessionTrackerProvider,
      Provider<AppStrings> appStringsProvider, Provider<CoroutineScope> applicationScopeProvider) {
    return new PlayerViewModel_Factory(savedStateHandleProvider, playbackRepositoryProvider, playerManagerProvider, watchHistoryRepositoryProvider, appSessionRepositoryProvider, userSettingsRepositoryProvider, catalogRepositoryProvider, nextEpisodeResolverProvider, episodeCatalogRepositoryProvider, playbackSessionTrackerProvider, appStringsProvider, applicationScopeProvider);
  }

  public static PlayerViewModel newInstance(SavedStateHandle savedStateHandle,
      PlaybackRepository playbackRepository, PlayerManager playerManager,
      WatchHistoryRepository watchHistoryRepository, AppSessionRepository appSessionRepository,
      UserSettingsRepository userSettingsRepository, CatalogRepository catalogRepository,
      NextEpisodeResolver nextEpisodeResolver, EpisodeCatalogRepository episodeCatalogRepository,
      PlaybackSessionTracker playbackSessionTracker, AppStrings appStrings,
      CoroutineScope applicationScope) {
    return new PlayerViewModel(savedStateHandle, playbackRepository, playerManager, watchHistoryRepository, appSessionRepository, userSettingsRepository, catalogRepository, nextEpisodeResolver, episodeCatalogRepository, playbackSessionTracker, appStrings, applicationScope);
  }
}
