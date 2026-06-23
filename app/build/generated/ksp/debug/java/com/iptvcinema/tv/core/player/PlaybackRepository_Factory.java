package com.iptvcinema.tv.core.player;

import com.iptvcinema.tv.core.data.local.LocalCredentialsStore;
import com.iptvcinema.tv.core.data.repository.CatalogRepository;
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
public final class PlaybackRepository_Factory implements Factory<PlaybackRepository> {
  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<LocalCredentialsStore> localCredentialsStoreProvider;

  private final Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider;

  public PlaybackRepository_Factory(Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider) {
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.localCredentialsStoreProvider = localCredentialsStoreProvider;
    this.episodeCatalogRepositoryProvider = episodeCatalogRepositoryProvider;
  }

  @Override
  public PlaybackRepository get() {
    return newInstance(catalogRepositoryProvider.get(), appSessionRepositoryProvider.get(), localCredentialsStoreProvider.get(), episodeCatalogRepositoryProvider.get());
  }

  public static PlaybackRepository_Factory create(
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      javax.inject.Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider) {
    return new PlaybackRepository_Factory(Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(localCredentialsStoreProvider), Providers.asDaggerProvider(episodeCatalogRepositoryProvider));
  }

  public static PlaybackRepository_Factory create(
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider) {
    return new PlaybackRepository_Factory(catalogRepositoryProvider, appSessionRepositoryProvider, localCredentialsStoreProvider, episodeCatalogRepositoryProvider);
  }

  public static PlaybackRepository newInstance(CatalogRepository catalogRepository,
      AppSessionRepository appSessionRepository, LocalCredentialsStore localCredentialsStore,
      EpisodeCatalogRepository episodeCatalogRepository) {
    return new PlaybackRepository(catalogRepository, appSessionRepository, localCredentialsStore, episodeCatalogRepository);
  }
}
