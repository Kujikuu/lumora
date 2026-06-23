package com.iptvcinema.tv.core.m3u;

import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository;
import com.iptvcinema.tv.core.database.CatalogDaoFacade;
import com.iptvcinema.tv.core.epg.EpgSyncRepository;
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
public final class M3uSyncRepository_Factory implements Factory<M3uSyncRepository> {
  private final Provider<M3uDownloader> m3uDownloaderProvider;

  private final Provider<CatalogDaoFacade> catalogDaoFacadeProvider;

  private final Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider;

  private final Provider<EpgSyncRepository> epgSyncRepositoryProvider;

  public M3uSyncRepository_Factory(Provider<M3uDownloader> m3uDownloaderProvider,
      Provider<CatalogDaoFacade> catalogDaoFacadeProvider,
      Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      Provider<EpgSyncRepository> epgSyncRepositoryProvider) {
    this.m3uDownloaderProvider = m3uDownloaderProvider;
    this.catalogDaoFacadeProvider = catalogDaoFacadeProvider;
    this.playlistSourcesRepositoryProvider = playlistSourcesRepositoryProvider;
    this.epgSyncRepositoryProvider = epgSyncRepositoryProvider;
  }

  @Override
  public M3uSyncRepository get() {
    return newInstance(m3uDownloaderProvider.get(), catalogDaoFacadeProvider.get(), playlistSourcesRepositoryProvider.get(), epgSyncRepositoryProvider.get());
  }

  public static M3uSyncRepository_Factory create(
      javax.inject.Provider<M3uDownloader> m3uDownloaderProvider,
      javax.inject.Provider<CatalogDaoFacade> catalogDaoFacadeProvider,
      javax.inject.Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      javax.inject.Provider<EpgSyncRepository> epgSyncRepositoryProvider) {
    return new M3uSyncRepository_Factory(Providers.asDaggerProvider(m3uDownloaderProvider), Providers.asDaggerProvider(catalogDaoFacadeProvider), Providers.asDaggerProvider(playlistSourcesRepositoryProvider), Providers.asDaggerProvider(epgSyncRepositoryProvider));
  }

  public static M3uSyncRepository_Factory create(Provider<M3uDownloader> m3uDownloaderProvider,
      Provider<CatalogDaoFacade> catalogDaoFacadeProvider,
      Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      Provider<EpgSyncRepository> epgSyncRepositoryProvider) {
    return new M3uSyncRepository_Factory(m3uDownloaderProvider, catalogDaoFacadeProvider, playlistSourcesRepositoryProvider, epgSyncRepositoryProvider);
  }

  public static M3uSyncRepository newInstance(M3uDownloader m3uDownloader,
      CatalogDaoFacade catalogDaoFacade, PlaylistSourcesRepository playlistSourcesRepository,
      EpgSyncRepository epgSyncRepository) {
    return new M3uSyncRepository(m3uDownloader, catalogDaoFacade, playlistSourcesRepository, epgSyncRepository);
  }
}
