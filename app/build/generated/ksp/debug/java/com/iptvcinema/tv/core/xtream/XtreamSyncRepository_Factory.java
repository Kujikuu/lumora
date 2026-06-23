package com.iptvcinema.tv.core.xtream;

import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository;
import com.iptvcinema.tv.core.database.CatalogDaoFacade;
import com.iptvcinema.tv.core.epg.EpgSyncRepository;
import com.iptvcinema.tv.core.player.EpisodeCatalogRepository;
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
public final class XtreamSyncRepository_Factory implements Factory<XtreamSyncRepository> {
  private final Provider<XtreamRepository> xtreamRepositoryProvider;

  private final Provider<CatalogDaoFacade> catalogDaoFacadeProvider;

  private final Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider;

  private final Provider<EpgSyncRepository> epgSyncRepositoryProvider;

  private final Provider<WatchedSeriesEpisodePrefetcher> watchedSeriesEpisodePrefetcherProvider;

  private final Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider;

  public XtreamSyncRepository_Factory(Provider<XtreamRepository> xtreamRepositoryProvider,
      Provider<CatalogDaoFacade> catalogDaoFacadeProvider,
      Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      Provider<EpgSyncRepository> epgSyncRepositoryProvider,
      Provider<WatchedSeriesEpisodePrefetcher> watchedSeriesEpisodePrefetcherProvider,
      Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider) {
    this.xtreamRepositoryProvider = xtreamRepositoryProvider;
    this.catalogDaoFacadeProvider = catalogDaoFacadeProvider;
    this.playlistSourcesRepositoryProvider = playlistSourcesRepositoryProvider;
    this.epgSyncRepositoryProvider = epgSyncRepositoryProvider;
    this.watchedSeriesEpisodePrefetcherProvider = watchedSeriesEpisodePrefetcherProvider;
    this.episodeCatalogRepositoryProvider = episodeCatalogRepositoryProvider;
  }

  @Override
  public XtreamSyncRepository get() {
    return newInstance(xtreamRepositoryProvider.get(), catalogDaoFacadeProvider.get(), playlistSourcesRepositoryProvider.get(), epgSyncRepositoryProvider.get(), watchedSeriesEpisodePrefetcherProvider.get(), episodeCatalogRepositoryProvider.get());
  }

  public static XtreamSyncRepository_Factory create(
      javax.inject.Provider<XtreamRepository> xtreamRepositoryProvider,
      javax.inject.Provider<CatalogDaoFacade> catalogDaoFacadeProvider,
      javax.inject.Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      javax.inject.Provider<EpgSyncRepository> epgSyncRepositoryProvider,
      javax.inject.Provider<WatchedSeriesEpisodePrefetcher> watchedSeriesEpisodePrefetcherProvider,
      javax.inject.Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider) {
    return new XtreamSyncRepository_Factory(Providers.asDaggerProvider(xtreamRepositoryProvider), Providers.asDaggerProvider(catalogDaoFacadeProvider), Providers.asDaggerProvider(playlistSourcesRepositoryProvider), Providers.asDaggerProvider(epgSyncRepositoryProvider), Providers.asDaggerProvider(watchedSeriesEpisodePrefetcherProvider), Providers.asDaggerProvider(episodeCatalogRepositoryProvider));
  }

  public static XtreamSyncRepository_Factory create(
      Provider<XtreamRepository> xtreamRepositoryProvider,
      Provider<CatalogDaoFacade> catalogDaoFacadeProvider,
      Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      Provider<EpgSyncRepository> epgSyncRepositoryProvider,
      Provider<WatchedSeriesEpisodePrefetcher> watchedSeriesEpisodePrefetcherProvider,
      Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider) {
    return new XtreamSyncRepository_Factory(xtreamRepositoryProvider, catalogDaoFacadeProvider, playlistSourcesRepositoryProvider, epgSyncRepositoryProvider, watchedSeriesEpisodePrefetcherProvider, episodeCatalogRepositoryProvider);
  }

  public static XtreamSyncRepository newInstance(XtreamRepository xtreamRepository,
      CatalogDaoFacade catalogDaoFacade, PlaylistSourcesRepository playlistSourcesRepository,
      EpgSyncRepository epgSyncRepository,
      WatchedSeriesEpisodePrefetcher watchedSeriesEpisodePrefetcher,
      EpisodeCatalogRepository episodeCatalogRepository) {
    return new XtreamSyncRepository(xtreamRepository, catalogDaoFacade, playlistSourcesRepository, epgSyncRepository, watchedSeriesEpisodePrefetcher, episodeCatalogRepository);
  }
}
