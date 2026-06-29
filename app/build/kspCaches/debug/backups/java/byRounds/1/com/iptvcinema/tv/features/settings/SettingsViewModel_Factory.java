package com.iptvcinema.tv.features.settings;

import com.iptvcinema.tv.core.catalog.CatalogRefreshController;
import com.iptvcinema.tv.core.data.local.LocalCredentialsStore;
import com.iptvcinema.tv.core.data.repository.AuthRepository;
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository;
import com.iptvcinema.tv.core.data.repository.UserSettingsRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import com.iptvcinema.tv.core.parental.ParentalGate;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<UserSettingsRepository> userSettingsRepositoryProvider;

  private final Provider<ParentalControlsRepository> parentalControlsRepositoryProvider;

  private final Provider<ParentalGate> parentalGateProvider;

  private final Provider<LocalCredentialsStore> localCredentialsStoreProvider;

  private final Provider<CatalogRefreshController> catalogRefreshControllerProvider;

  public SettingsViewModel_Factory(Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      Provider<CatalogRefreshController> catalogRefreshControllerProvider) {
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.userSettingsRepositoryProvider = userSettingsRepositoryProvider;
    this.parentalControlsRepositoryProvider = parentalControlsRepositoryProvider;
    this.parentalGateProvider = parentalGateProvider;
    this.localCredentialsStoreProvider = localCredentialsStoreProvider;
    this.catalogRefreshControllerProvider = catalogRefreshControllerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(appSessionRepositoryProvider.get(), authRepositoryProvider.get(), userSettingsRepositoryProvider.get(), parentalControlsRepositoryProvider.get(), parentalGateProvider.get(), localCredentialsStoreProvider.get(), catalogRefreshControllerProvider.get());
  }

  public static SettingsViewModel_Factory create(
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      javax.inject.Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      javax.inject.Provider<ParentalGate> parentalGateProvider,
      javax.inject.Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      javax.inject.Provider<CatalogRefreshController> catalogRefreshControllerProvider) {
    return new SettingsViewModel_Factory(Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(userSettingsRepositoryProvider), Providers.asDaggerProvider(parentalControlsRepositoryProvider), Providers.asDaggerProvider(parentalGateProvider), Providers.asDaggerProvider(localCredentialsStoreProvider), Providers.asDaggerProvider(catalogRefreshControllerProvider));
  }

  public static SettingsViewModel_Factory create(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserSettingsRepository> userSettingsRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      Provider<CatalogRefreshController> catalogRefreshControllerProvider) {
    return new SettingsViewModel_Factory(appSessionRepositoryProvider, authRepositoryProvider, userSettingsRepositoryProvider, parentalControlsRepositoryProvider, parentalGateProvider, localCredentialsStoreProvider, catalogRefreshControllerProvider);
  }

  public static SettingsViewModel newInstance(AppSessionRepository appSessionRepository,
      AuthRepository authRepository, UserSettingsRepository userSettingsRepository,
      ParentalControlsRepository parentalControlsRepository, ParentalGate parentalGate,
      LocalCredentialsStore localCredentialsStore,
      CatalogRefreshController catalogRefreshController) {
    return new SettingsViewModel(appSessionRepository, authRepository, userSettingsRepository, parentalControlsRepository, parentalGate, localCredentialsStore, catalogRefreshController);
  }
}
