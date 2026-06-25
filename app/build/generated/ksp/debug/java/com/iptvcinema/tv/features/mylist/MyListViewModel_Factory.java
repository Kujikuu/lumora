package com.iptvcinema.tv.features.mylist;

import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.data.repository.FavoritesRepository;
import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
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
public final class MyListViewModel_Factory implements Factory<MyListViewModel> {
  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<FavoritesRepository> favoritesRepositoryProvider;

  private final Provider<WatchHistoryRepository> watchHistoryRepositoryProvider;

  private final Provider<AppStrings> appStringsProvider;

  public MyListViewModel_Factory(Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      Provider<AppStrings> appStringsProvider) {
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.favoritesRepositoryProvider = favoritesRepositoryProvider;
    this.watchHistoryRepositoryProvider = watchHistoryRepositoryProvider;
    this.appStringsProvider = appStringsProvider;
  }

  @Override
  public MyListViewModel get() {
    return newInstance(appSessionRepositoryProvider.get(), catalogRepositoryProvider.get(), favoritesRepositoryProvider.get(), watchHistoryRepositoryProvider.get(), appStringsProvider.get());
  }

  public static MyListViewModel_Factory create(
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<FavoritesRepository> favoritesRepositoryProvider,
      javax.inject.Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      javax.inject.Provider<AppStrings> appStringsProvider) {
    return new MyListViewModel_Factory(Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(favoritesRepositoryProvider), Providers.asDaggerProvider(watchHistoryRepositoryProvider), Providers.asDaggerProvider(appStringsProvider));
  }

  public static MyListViewModel_Factory create(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      Provider<AppStrings> appStringsProvider) {
    return new MyListViewModel_Factory(appSessionRepositoryProvider, catalogRepositoryProvider, favoritesRepositoryProvider, watchHistoryRepositoryProvider, appStringsProvider);
  }

  public static MyListViewModel newInstance(AppSessionRepository appSessionRepository,
      CatalogRepository catalogRepository, FavoritesRepository favoritesRepository,
      WatchHistoryRepository watchHistoryRepository, AppStrings appStrings) {
    return new MyListViewModel(appSessionRepository, catalogRepository, favoritesRepository, watchHistoryRepository, appStrings);
  }
}
