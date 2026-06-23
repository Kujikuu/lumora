package com.iptvcinema.tv.core.navigation;

import com.iptvcinema.tv.core.datastore.AppSessionRepository;
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
public final class SessionViewModel_Factory implements Factory<SessionViewModel> {
  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  public SessionViewModel_Factory(Provider<AppSessionRepository> appSessionRepositoryProvider) {
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
  }

  @Override
  public SessionViewModel get() {
    return newInstance(appSessionRepositoryProvider.get());
  }

  public static SessionViewModel_Factory create(
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider) {
    return new SessionViewModel_Factory(Providers.asDaggerProvider(appSessionRepositoryProvider));
  }

  public static SessionViewModel_Factory create(
      Provider<AppSessionRepository> appSessionRepositoryProvider) {
    return new SessionViewModel_Factory(appSessionRepositoryProvider);
  }

  public static SessionViewModel newInstance(AppSessionRepository appSessionRepository) {
    return new SessionViewModel(appSessionRepository);
  }
}
