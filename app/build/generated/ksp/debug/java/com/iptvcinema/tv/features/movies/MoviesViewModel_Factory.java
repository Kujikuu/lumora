package com.iptvcinema.tv.features.movies;

import com.iptvcinema.tv.core.catalog.CatalogRefreshController;
import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.data.repository.FavoritesRepository;
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository;
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository;
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import com.iptvcinema.tv.core.parental.ParentalGate;
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
public final class MoviesViewModel_Factory implements Factory<MoviesViewModel> {
  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<WatchHistoryRepository> watchHistoryRepositoryProvider;

  private final Provider<FavoritesRepository> favoritesRepositoryProvider;

  private final Provider<UserSettingsRepository> userSettingsRepositoryProvider;

  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<ParentalControlsRepository> parentalControlsRepositoryProvider;

  private final Provider<ParentalGate> parentalGateProvider;

  private final Provider<CatalogRefreshController> catalogRefreshControllerProvider;

  public MoviesViewModel_Factory(Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider,
      Provider<CatalogRefreshController> catalogRefreshControllerProvider) {
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.watchHistoryRepositoryProvider = watchHistoryRepositoryProvider;
    this.favoritesRepositoryProvider = favoritesRepositoryProvider;
    this.userSettingsRepositoryProvider = userSettingsRepositoryProvider;
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.parentalControlsRepositoryProvider = parentalControlsRepositoryProvider;
    this.parentalGateProvider = parentalGateProvider;
    this.catalogRefreshControllerProvider = catalogRefreshControllerProvider;
  }

  @Override
  public MoviesViewModel get() {
    return newInstance(catalogRepositoryProvider.get(), watchHistoryRepositoryProvider.get(), favoritesRepositoryProvider.get(), userSettingsRepositoryProvider.get(), appSessionRepositoryProvider.get(), parentalControlsRepositoryProvider.get(), parentalGateProvider.get(), catalogRefreshControllerProvider.get());
  }

  public static MoviesViewModel_Factory create(
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      javax.inject.Provider<FavoritesRepository> favoritesRepositoryProvider,
      javax.inject.Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      javax.inject.Provider<ParentalGate> parentalGateProvider,
      javax.inject.Provider<CatalogRefreshController> catalogRefreshControllerProvider) {
    return new MoviesViewModel_Factory(Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(watchHistoryRepositoryProvider), Providers.asDaggerProvider(favoritesRepositoryProvider), Providers.asDaggerProvider(userSettingsRepositoryProvider), Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(parentalControlsRepositoryProvider), Providers.asDaggerProvider(parentalGateProvider), Providers.asDaggerProvider(catalogRefreshControllerProvider));
  }

  public static MoviesViewModel_Factory create(
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider,
      Provider<CatalogRefreshController> catalogRefreshControllerProvider) {
    return new MoviesViewModel_Factory(catalogRepositoryProvider, watchHistoryRepositoryProvider, favoritesRepositoryProvider, userSettingsRepositoryProvider, appSessionRepositoryProvider, parentalControlsRepositoryProvider, parentalGateProvider, catalogRefreshControllerProvider);
  }

  public static MoviesViewModel newInstance(CatalogRepository catalogRepository,
      WatchHistoryRepository watchHistoryRepository, FavoritesRepository favoritesRepository,
      UserSettingsRepository userSettingsRepository, AppSessionRepository appSessionRepository,
      ParentalControlsRepository parentalControlsRepository, ParentalGate parentalGate,
      CatalogRefreshController catalogRefreshController) {
    return new MoviesViewModel(catalogRepository, watchHistoryRepository, favoritesRepository, userSettingsRepository, appSessionRepository, parentalControlsRepository, parentalGate, catalogRefreshController);
  }
}
