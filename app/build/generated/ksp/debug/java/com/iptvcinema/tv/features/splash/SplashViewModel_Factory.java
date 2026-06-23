package com.iptvcinema.tv.features.splash;

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
public final class SplashViewModel_Factory implements Factory<SplashViewModel> {
  private final Provider<StartupSessionBootstrap> startupSessionBootstrapProvider;

  public SplashViewModel_Factory(
      Provider<StartupSessionBootstrap> startupSessionBootstrapProvider) {
    this.startupSessionBootstrapProvider = startupSessionBootstrapProvider;
  }

  @Override
  public SplashViewModel get() {
    return newInstance(startupSessionBootstrapProvider.get());
  }

  public static SplashViewModel_Factory create(
      javax.inject.Provider<StartupSessionBootstrap> startupSessionBootstrapProvider) {
    return new SplashViewModel_Factory(Providers.asDaggerProvider(startupSessionBootstrapProvider));
  }

  public static SplashViewModel_Factory create(
      Provider<StartupSessionBootstrap> startupSessionBootstrapProvider) {
    return new SplashViewModel_Factory(startupSessionBootstrapProvider);
  }

  public static SplashViewModel newInstance(StartupSessionBootstrap startupSessionBootstrap) {
    return new SplashViewModel(startupSessionBootstrap);
  }
}
