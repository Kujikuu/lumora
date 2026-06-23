package com.iptvcinema.tv.features.profiles;

import com.iptvcinema.tv.core.data.repository.ProfilesRepository;
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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<ProfilesRepository> profilesRepositoryProvider;

  private final Provider<ParentalGate> parentalGateProvider;

  public ProfileViewModel_Factory(Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<ProfilesRepository> profilesRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider) {
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.profilesRepositoryProvider = profilesRepositoryProvider;
    this.parentalGateProvider = parentalGateProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(appSessionRepositoryProvider.get(), profilesRepositoryProvider.get(), parentalGateProvider.get());
  }

  public static ProfileViewModel_Factory create(
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<ProfilesRepository> profilesRepositoryProvider,
      javax.inject.Provider<ParentalGate> parentalGateProvider) {
    return new ProfileViewModel_Factory(Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(profilesRepositoryProvider), Providers.asDaggerProvider(parentalGateProvider));
  }

  public static ProfileViewModel_Factory create(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<ProfilesRepository> profilesRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider) {
    return new ProfileViewModel_Factory(appSessionRepositoryProvider, profilesRepositoryProvider, parentalGateProvider);
  }

  public static ProfileViewModel newInstance(AppSessionRepository appSessionRepository,
      ProfilesRepository profilesRepository, ParentalGate parentalGate) {
    return new ProfileViewModel(appSessionRepository, profilesRepository, parentalGate);
  }
}
