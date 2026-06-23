package com.iptvcinema.tv.features.sources;

import com.iptvcinema.tv.core.data.local.LocalCredentialsStore;
import com.iptvcinema.tv.core.data.repository.AuthRepository;
import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.data.repository.PlaylistSourcesRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import com.iptvcinema.tv.core.m3u.M3uSyncRepository;
import com.iptvcinema.tv.core.xtream.XtreamRepository;
import com.iptvcinema.tv.core.xtream.XtreamSyncRepository;
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
public final class SourceViewModel_Factory implements Factory<SourceViewModel> {
  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider;

  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<XtreamRepository> xtreamRepositoryProvider;

  private final Provider<XtreamSyncRepository> xtreamSyncRepositoryProvider;

  private final Provider<M3uSyncRepository> m3uSyncRepositoryProvider;

  private final Provider<LocalCredentialsStore> localCredentialsStoreProvider;

  public SourceViewModel_Factory(Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<XtreamRepository> xtreamRepositoryProvider,
      Provider<XtreamSyncRepository> xtreamSyncRepositoryProvider,
      Provider<M3uSyncRepository> m3uSyncRepositoryProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider) {
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.playlistSourcesRepositoryProvider = playlistSourcesRepositoryProvider;
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.xtreamRepositoryProvider = xtreamRepositoryProvider;
    this.xtreamSyncRepositoryProvider = xtreamSyncRepositoryProvider;
    this.m3uSyncRepositoryProvider = m3uSyncRepositoryProvider;
    this.localCredentialsStoreProvider = localCredentialsStoreProvider;
  }

  @Override
  public SourceViewModel get() {
    return newInstance(appSessionRepositoryProvider.get(), playlistSourcesRepositoryProvider.get(), catalogRepositoryProvider.get(), authRepositoryProvider.get(), xtreamRepositoryProvider.get(), xtreamSyncRepositoryProvider.get(), m3uSyncRepositoryProvider.get(), localCredentialsStoreProvider.get());
  }

  public static SourceViewModel_Factory create(
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<XtreamRepository> xtreamRepositoryProvider,
      javax.inject.Provider<XtreamSyncRepository> xtreamSyncRepositoryProvider,
      javax.inject.Provider<M3uSyncRepository> m3uSyncRepositoryProvider,
      javax.inject.Provider<LocalCredentialsStore> localCredentialsStoreProvider) {
    return new SourceViewModel_Factory(Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(playlistSourcesRepositoryProvider), Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(xtreamRepositoryProvider), Providers.asDaggerProvider(xtreamSyncRepositoryProvider), Providers.asDaggerProvider(m3uSyncRepositoryProvider), Providers.asDaggerProvider(localCredentialsStoreProvider));
  }

  public static SourceViewModel_Factory create(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<PlaylistSourcesRepository> playlistSourcesRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<XtreamRepository> xtreamRepositoryProvider,
      Provider<XtreamSyncRepository> xtreamSyncRepositoryProvider,
      Provider<M3uSyncRepository> m3uSyncRepositoryProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider) {
    return new SourceViewModel_Factory(appSessionRepositoryProvider, playlistSourcesRepositoryProvider, catalogRepositoryProvider, authRepositoryProvider, xtreamRepositoryProvider, xtreamSyncRepositoryProvider, m3uSyncRepositoryProvider, localCredentialsStoreProvider);
  }

  public static SourceViewModel newInstance(AppSessionRepository appSessionRepository,
      PlaylistSourcesRepository playlistSourcesRepository, CatalogRepository catalogRepository,
      AuthRepository authRepository, XtreamRepository xtreamRepository,
      XtreamSyncRepository xtreamSyncRepository, M3uSyncRepository m3uSyncRepository,
      LocalCredentialsStore localCredentialsStore) {
    return new SourceViewModel(appSessionRepository, playlistSourcesRepository, catalogRepository, authRepository, xtreamRepository, xtreamSyncRepository, m3uSyncRepository, localCredentialsStore);
  }
}
