package com.iptvcinema.tv.features.details;

import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.data.repository.FavoritesRepository;
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import com.iptvcinema.tv.core.parental.ParentalGate;
import com.iptvcinema.tv.core.player.EpisodeCatalogRepository;
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
public final class DetailsViewModel_Factory implements Factory<DetailsViewModel> {
  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<FavoritesRepository> favoritesRepositoryProvider;

  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider;

  private final Provider<ParentalControlsRepository> parentalControlsRepositoryProvider;

  private final Provider<ParentalGate> parentalGateProvider;

  private final Provider<AppStrings> appStringsProvider;

  public DetailsViewModel_Factory(Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider, Provider<AppStrings> appStringsProvider) {
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.favoritesRepositoryProvider = favoritesRepositoryProvider;
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.episodeCatalogRepositoryProvider = episodeCatalogRepositoryProvider;
    this.parentalControlsRepositoryProvider = parentalControlsRepositoryProvider;
    this.parentalGateProvider = parentalGateProvider;
    this.appStringsProvider = appStringsProvider;
  }

  @Override
  public DetailsViewModel get() {
    return newInstance(appSessionRepositoryProvider.get(), favoritesRepositoryProvider.get(), catalogRepositoryProvider.get(), episodeCatalogRepositoryProvider.get(), parentalControlsRepositoryProvider.get(), parentalGateProvider.get(), appStringsProvider.get());
  }

  public static DetailsViewModel_Factory create(
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<FavoritesRepository> favoritesRepositoryProvider,
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider,
      javax.inject.Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      javax.inject.Provider<ParentalGate> parentalGateProvider,
      javax.inject.Provider<AppStrings> appStringsProvider) {
    return new DetailsViewModel_Factory(Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(favoritesRepositoryProvider), Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(episodeCatalogRepositoryProvider), Providers.asDaggerProvider(parentalControlsRepositoryProvider), Providers.asDaggerProvider(parentalGateProvider), Providers.asDaggerProvider(appStringsProvider));
  }

  public static DetailsViewModel_Factory create(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider, Provider<AppStrings> appStringsProvider) {
    return new DetailsViewModel_Factory(appSessionRepositoryProvider, favoritesRepositoryProvider, catalogRepositoryProvider, episodeCatalogRepositoryProvider, parentalControlsRepositoryProvider, parentalGateProvider, appStringsProvider);
  }

  public static DetailsViewModel newInstance(AppSessionRepository appSessionRepository,
      FavoritesRepository favoritesRepository, CatalogRepository catalogRepository,
      EpisodeCatalogRepository episodeCatalogRepository,
      ParentalControlsRepository parentalControlsRepository, ParentalGate parentalGate,
      AppStrings appStrings) {
    return new DetailsViewModel(appSessionRepository, favoritesRepository, catalogRepository, episodeCatalogRepository, parentalControlsRepository, parentalGate, appStrings);
  }
}
