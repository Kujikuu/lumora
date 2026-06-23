package com.iptvcinema.tv.core.data.repository;

import com.iptvcinema.tv.core.database.CatalogDaoFacade;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import com.iptvcinema.tv.core.util.AppStrings;
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
public final class CatalogRepository_Factory implements Factory<CatalogRepository> {
  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<CatalogDaoFacade> catalogDaoFacadeProvider;

  private final Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider;

  private final Provider<AppStrings> appStringsProvider;

  public CatalogRepository_Factory(Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<CatalogDaoFacade> catalogDaoFacadeProvider,
      Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      Provider<AppStrings> appStringsProvider) {
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.catalogDaoFacadeProvider = catalogDaoFacadeProvider;
    this.playlistSourcesRepositoryProvider = playlistSourcesRepositoryProvider;
    this.appStringsProvider = appStringsProvider;
  }

  @Override
  public CatalogRepository get() {
    return newInstance(appSessionRepositoryProvider.get(), catalogDaoFacadeProvider.get(), playlistSourcesRepositoryProvider.get(), appStringsProvider.get());
  }

  public static CatalogRepository_Factory create(
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<CatalogDaoFacade> catalogDaoFacadeProvider,
      javax.inject.Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      javax.inject.Provider<AppStrings> appStringsProvider) {
    return new CatalogRepository_Factory(Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(catalogDaoFacadeProvider), Providers.asDaggerProvider(playlistSourcesRepositoryProvider), Providers.asDaggerProvider(appStringsProvider));
  }

  public static CatalogRepository_Factory create(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<CatalogDaoFacade> catalogDaoFacadeProvider,
      Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      Provider<AppStrings> appStringsProvider) {
    return new CatalogRepository_Factory(appSessionRepositoryProvider, catalogDaoFacadeProvider, playlistSourcesRepositoryProvider, appStringsProvider);
  }

  public static CatalogRepository newInstance(AppSessionRepository appSessionRepository,
      CatalogDaoFacade catalogDaoFacade, PlaylistSourcesRepository playlistSourcesRepository,
      AppStrings appStrings) {
    return new CatalogRepository(appSessionRepository, catalogDaoFacade, playlistSourcesRepository, appStrings);
  }
}
