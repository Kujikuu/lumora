package com.iptvcinema.tv.features.series;

import com.iptvcinema.tv.core.catalog.CatalogRefreshController;
import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository;
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
public final class SeriesViewModel_Factory implements Factory<SeriesViewModel> {
  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<ParentalControlsRepository> parentalControlsRepositoryProvider;

  private final Provider<ParentalGate> parentalGateProvider;

  private final Provider<CatalogRefreshController> catalogRefreshControllerProvider;

  public SeriesViewModel_Factory(Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider,
      Provider<CatalogRefreshController> catalogRefreshControllerProvider) {
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.parentalControlsRepositoryProvider = parentalControlsRepositoryProvider;
    this.parentalGateProvider = parentalGateProvider;
    this.catalogRefreshControllerProvider = catalogRefreshControllerProvider;
  }

  @Override
  public SeriesViewModel get() {
    return newInstance(catalogRepositoryProvider.get(), appSessionRepositoryProvider.get(), parentalControlsRepositoryProvider.get(), parentalGateProvider.get(), catalogRefreshControllerProvider.get());
  }

  public static SeriesViewModel_Factory create(
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      javax.inject.Provider<ParentalGate> parentalGateProvider,
      javax.inject.Provider<CatalogRefreshController> catalogRefreshControllerProvider) {
    return new SeriesViewModel_Factory(Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(parentalControlsRepositoryProvider), Providers.asDaggerProvider(parentalGateProvider), Providers.asDaggerProvider(catalogRefreshControllerProvider));
  }

  public static SeriesViewModel_Factory create(
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider,
      Provider<CatalogRefreshController> catalogRefreshControllerProvider) {
    return new SeriesViewModel_Factory(catalogRepositoryProvider, appSessionRepositoryProvider, parentalControlsRepositoryProvider, parentalGateProvider, catalogRefreshControllerProvider);
  }

  public static SeriesViewModel newInstance(CatalogRepository catalogRepository,
      AppSessionRepository appSessionRepository,
      ParentalControlsRepository parentalControlsRepository, ParentalGate parentalGate,
      CatalogRefreshController catalogRefreshController) {
    return new SeriesViewModel(catalogRepository, appSessionRepository, parentalControlsRepository, parentalGate, catalogRefreshController);
  }
}
