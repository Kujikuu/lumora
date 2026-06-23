package com.iptvcinema.tv.core.player;

import com.iptvcinema.tv.core.data.repository.WatchHistoryRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
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
public final class WatchedSeriesEpisodePrefetcher_Factory implements Factory<WatchedSeriesEpisodePrefetcher> {
  private final Provider<WatchHistoryRepository> watchHistoryRepositoryProvider;

  private final Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider;

  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  public WatchedSeriesEpisodePrefetcher_Factory(
      Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider) {
    this.watchHistoryRepositoryProvider = watchHistoryRepositoryProvider;
    this.episodeCatalogRepositoryProvider = episodeCatalogRepositoryProvider;
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
  }

  @Override
  public WatchedSeriesEpisodePrefetcher get() {
    return newInstance(watchHistoryRepositoryProvider.get(), episodeCatalogRepositoryProvider.get(), appSessionRepositoryProvider.get());
  }

  public static WatchedSeriesEpisodePrefetcher_Factory create(
      javax.inject.Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      javax.inject.Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider,
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider) {
    return new WatchedSeriesEpisodePrefetcher_Factory(Providers.asDaggerProvider(watchHistoryRepositoryProvider), Providers.asDaggerProvider(episodeCatalogRepositoryProvider), Providers.asDaggerProvider(appSessionRepositoryProvider));
  }

  public static WatchedSeriesEpisodePrefetcher_Factory create(
      Provider<WatchHistoryRepository> watchHistoryRepositoryProvider,
      Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider) {
    return new WatchedSeriesEpisodePrefetcher_Factory(watchHistoryRepositoryProvider, episodeCatalogRepositoryProvider, appSessionRepositoryProvider);
  }

  public static WatchedSeriesEpisodePrefetcher newInstance(
      WatchHistoryRepository watchHistoryRepository,
      EpisodeCatalogRepository episodeCatalogRepository,
      AppSessionRepository appSessionRepository) {
    return new WatchedSeriesEpisodePrefetcher(watchHistoryRepository, episodeCatalogRepository, appSessionRepository);
  }
}
