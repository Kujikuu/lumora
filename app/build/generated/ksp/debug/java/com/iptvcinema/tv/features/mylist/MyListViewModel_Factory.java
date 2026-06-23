package com.iptvcinema.tv.features.mylist;

import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.data.repository.FavoritesRepository;
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
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
public final class MyListViewModel_Factory implements Factory<MyListViewModel> {
  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<FavoritesRepository> favoritesRepositoryProvider;

  private final Provider<WatchHistoryRepository> watchHistoryRepositoryProvider;

  public MyListViewModel_Factory(Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<WatchHistoryRepository> watchHistoryRepositoryProvider) {
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.favoritesRepositoryProvider = favoritesRepositoryProvider;
    this.watchHistoryRepositoryProvider = watchHistoryRepositoryProvider;
  }

  @Override
  public MyListViewModel get() {
    return newInstance(appSessionRepositoryProvider.get(), catalogRepositoryProvider.get(), favoritesRepositoryProvider.get(), watchHistoryRepositoryProvider.get());
  }

  public static MyListViewModel_Factory create(
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<FavoritesRepository> favoritesRepositoryProvider,
      javax.inject.Provider<WatchHistoryRepository> watchHistoryRepositoryProvider) {
    return new MyListViewModel_Factory(Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(favoritesRepositoryProvider), Providers.asDaggerProvider(watchHistoryRepositoryProvider));
  }

  public static MyListViewModel_Factory create(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<WatchHistoryRepository> watchHistoryRepositoryProvider) {
    return new MyListViewModel_Factory(appSessionRepositoryProvider, catalogRepositoryProvider, favoritesRepositoryProvider, watchHistoryRepositoryProvider);
  }

  public static MyListViewModel newInstance(AppSessionRepository appSessionRepository,
      CatalogRepository catalogRepository, FavoritesRepository favoritesRepository,
      WatchHistoryRepository watchHistoryRepository) {
    return new MyListViewModel(appSessionRepository, catalogRepository, favoritesRepository, watchHistoryRepository);
  }
}
