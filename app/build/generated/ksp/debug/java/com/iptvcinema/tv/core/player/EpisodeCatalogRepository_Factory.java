package com.iptvcinema.tv.core.player;

import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import com.iptvcinema.tv.core.xtream.XtreamRepository;
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
public final class EpisodeCatalogRepository_Factory implements Factory<EpisodeCatalogRepository> {
  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<XtreamRepository> xtreamRepositoryProvider;

  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  public EpisodeCatalogRepository_Factory(Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<XtreamRepository> xtreamRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider) {
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.xtreamRepositoryProvider = xtreamRepositoryProvider;
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
  }

  @Override
  public EpisodeCatalogRepository get() {
    return newInstance(catalogRepositoryProvider.get(), xtreamRepositoryProvider.get(), appSessionRepositoryProvider.get());
  }

  public static EpisodeCatalogRepository_Factory create(
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<XtreamRepository> xtreamRepositoryProvider,
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider) {
    return new EpisodeCatalogRepository_Factory(Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(xtreamRepositoryProvider), Providers.asDaggerProvider(appSessionRepositoryProvider));
  }

  public static EpisodeCatalogRepository_Factory create(
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<XtreamRepository> xtreamRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider) {
    return new EpisodeCatalogRepository_Factory(catalogRepositoryProvider, xtreamRepositoryProvider, appSessionRepositoryProvider);
  }

  public static EpisodeCatalogRepository newInstance(CatalogRepository catalogRepository,
      XtreamRepository xtreamRepository, AppSessionRepository appSessionRepository) {
    return new EpisodeCatalogRepository(catalogRepository, xtreamRepository, appSessionRepository);
  }
}
