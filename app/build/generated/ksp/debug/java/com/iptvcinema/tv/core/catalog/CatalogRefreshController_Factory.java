package com.iptvcinema.tv.core.catalog;

import com.iptvcinema.tv.core.data.local.LocalCredentialsStore;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import com.iptvcinema.tv.core.m3u.M3uSyncRepository;
import com.iptvcinema.tv.core.util.AppStrings;
import com.iptvcinema.tv.core.xtream.XtreamSyncRepository;
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
public final class CatalogRefreshController_Factory implements Factory<CatalogRefreshController> {
  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<LocalCredentialsStore> localCredentialsStoreProvider;

  private final Provider<XtreamSyncRepository> xtreamSyncRepositoryProvider;

  private final Provider<M3uSyncRepository> m3uSyncRepositoryProvider;

  private final Provider<AppStrings> appStringsProvider;

  public CatalogRefreshController_Factory(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      Provider<XtreamSyncRepository> xtreamSyncRepositoryProvider,
      Provider<M3uSyncRepository> m3uSyncRepositoryProvider,
      Provider<AppStrings> appStringsProvider) {
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.localCredentialsStoreProvider = localCredentialsStoreProvider;
    this.xtreamSyncRepositoryProvider = xtreamSyncRepositoryProvider;
    this.m3uSyncRepositoryProvider = m3uSyncRepositoryProvider;
    this.appStringsProvider = appStringsProvider;
  }

  @Override
  public CatalogRefreshController get() {
    return newInstance(appSessionRepositoryProvider.get(), localCredentialsStoreProvider.get(), xtreamSyncRepositoryProvider.get(), m3uSyncRepositoryProvider.get(), appStringsProvider.get());
  }

  public static CatalogRefreshController_Factory create(
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      javax.inject.Provider<XtreamSyncRepository> xtreamSyncRepositoryProvider,
      javax.inject.Provider<M3uSyncRepository> m3uSyncRepositoryProvider,
      javax.inject.Provider<AppStrings> appStringsProvider) {
    return new CatalogRefreshController_Factory(Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(localCredentialsStoreProvider), Providers.asDaggerProvider(xtreamSyncRepositoryProvider), Providers.asDaggerProvider(m3uSyncRepositoryProvider), Providers.asDaggerProvider(appStringsProvider));
  }

  public static CatalogRefreshController_Factory create(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      Provider<XtreamSyncRepository> xtreamSyncRepositoryProvider,
      Provider<M3uSyncRepository> m3uSyncRepositoryProvider,
      Provider<AppStrings> appStringsProvider) {
    return new CatalogRefreshController_Factory(appSessionRepositoryProvider, localCredentialsStoreProvider, xtreamSyncRepositoryProvider, m3uSyncRepositoryProvider, appStringsProvider);
  }

  public static CatalogRefreshController newInstance(AppSessionRepository appSessionRepository,
      LocalCredentialsStore localCredentialsStore, XtreamSyncRepository xtreamSyncRepository,
      M3uSyncRepository m3uSyncRepository, AppStrings appStrings) {
    return new CatalogRefreshController(appSessionRepository, localCredentialsStore, xtreamSyncRepository, m3uSyncRepository, appStrings);
  }
}
