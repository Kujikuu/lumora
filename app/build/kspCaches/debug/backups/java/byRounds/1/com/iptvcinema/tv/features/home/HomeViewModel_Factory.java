package com.iptvcinema.tv.features.home;

import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository;
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository;
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import com.iptvcinema.tv.core.parental.ParentalGate;
import com.iptvcinema.tv.core.player.PlaybackSessionTracker;
import com.iptvcinema.tv.core.util.AppStrings;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<WatchHistoryRepository> watchHistoryRepositoryProvider;

  private final Provider<UserSettingsRepository> userSettingsRepositoryProvider;

  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<ParentalControlsRepository> parentalControlsRepositoryProvider;

  private final Provider<ParentalGate> parentalGateProvider;

  private final Provider<PlaybackSessionTracker> playbackSessionTrackerProvider;

  private final Provider<AppStrings> appStringsProvider;

  public HomeViewModel_Factory(Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider,
      Provider<PlaybackSessionTracker> playbackSessionTrackerProvider,
      Provider<AppStrings> appStringsProvider) {
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.watchHistoryRepositoryProvider = watchHistoryRepositoryProvider;
    this.userSettingsRepositoryProvider = userSettingsRepositoryProvider;
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.parentalControlsRepositoryProvider = parentalControlsRepositoryProvider;
    this.parentalGateProvider = parentalGateProvider;
    this.playbackSessionTrackerProvider = playbackSessionTrackerProvider;
    this.appStringsProvider = appStringsProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(catalogRepositoryProvider.get(), watchHistoryRepositoryProvider.get(), userSettingsRepositoryProvider.get(), appSessionRepositoryProvider.get(), parentalControlsRepositoryProvider.get(), parentalGateProvider.get(), playbackSessionTrackerProvider.get(), appStringsProvider.get());
  }

  public static HomeViewModel_Factory create(
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      javax.inject.Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      javax.inject.Provider<ParentalGate> parentalGateProvider,
      javax.inject.Provider<PlaybackSessionTracker> playbackSessionTrackerProvider,
      javax.inject.Provider<AppStrings> appStringsProvider) {
    return new HomeViewModel_Factory(Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(watchHistoryRepositoryProvider), Providers.asDaggerProvider(userSettingsRepositoryProvider), Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(parentalControlsRepositoryProvider), Providers.asDaggerProvider(parentalGateProvider), Providers.asDaggerProvider(playbackSessionTrackerProvider), Providers.asDaggerProvider(appStringsProvider));
  }

  public static HomeViewModel_Factory create(Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider,
      Provider<PlaybackSessionTracker> playbackSessionTrackerProvider,
      Provider<AppStrings> appStringsProvider) {
    return new HomeViewModel_Factory(catalogRepositoryProvider, watchHistoryRepositoryProvider, userSettingsRepositoryProvider, appSessionRepositoryProvider, parentalControlsRepositoryProvider, parentalGateProvider, playbackSessionTrackerProvider, appStringsProvider);
  }

  public static HomeViewModel newInstance(CatalogRepository catalogRepository,
      WatchHistoryRepository watchHistoryRepository, UserSettingsRepository userSettingsRepository,
      AppSessionRepository appSessionRepository,
      ParentalControlsRepository parentalControlsRepository, ParentalGate parentalGate,
      PlaybackSessionTracker playbackSessionTracker, AppStrings appStrings) {
    return new HomeViewModel(catalogRepository, watchHistoryRepository, userSettingsRepository, appSessionRepository, parentalControlsRepository, parentalGate, playbackSessionTracker, appStrings);
  }
}
