package com.iptvcinema.tv.features.activation;

import com.iptvcinema.tv.core.data.repository.AuthRepository;
import com.iptvcinema.tv.core.data.repository.DeviceActivationRepository;
import com.iptvcinema.tv.core.datastore.StartupSessionBootstrap;
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
public final class ActivationViewModel_Factory implements Factory<ActivationViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<DeviceActivationRepository> deviceActivationRepositoryProvider;

  private final Provider<StartupSessionBootstrap> startupSessionBootstrapProvider;

  public ActivationViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<DeviceActivationRepository> deviceActivationRepositoryProvider,
      Provider<StartupSessionBootstrap> startupSessionBootstrapProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.deviceActivationRepositoryProvider = deviceActivationRepositoryProvider;
    this.startupSessionBootstrapProvider = startupSessionBootstrapProvider;
  }

  @Override
  public ActivationViewModel get() {
    return newInstance(authRepositoryProvider.get(), deviceActivationRepositoryProvider.get(), startupSessionBootstrapProvider.get());
  }

  public static ActivationViewModel_Factory create(
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<DeviceActivationRepository> deviceActivationRepositoryProvider,
      javax.inject.Provider<StartupSessionBootstrap> startupSessionBootstrapProvider) {
    return new ActivationViewModel_Factory(Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(deviceActivationRepositoryProvider), Providers.asDaggerProvider(startupSessionBootstrapProvider));
  }

  public static ActivationViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<DeviceActivationRepository> deviceActivationRepositoryProvider,
      Provider<StartupSessionBootstrap> startupSessionBootstrapProvider) {
    return new ActivationViewModel_Factory(authRepositoryProvider, deviceActivationRepositoryProvider, startupSessionBootstrapProvider);
  }

  public static ActivationViewModel newInstance(AuthRepository authRepository,
      DeviceActivationRepository deviceActivationRepository,
      StartupSessionBootstrap startupSessionBootstrap) {
    return new ActivationViewModel(authRepository, deviceActivationRepository, startupSessionBootstrap);
  }
}
