package com.iptvcinema.tv.features.parental;

import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository;
import com.iptvcinema.tv.core.data.repository.ProfilesRepository;
import com.iptvcinema.tv.core.parental.ParentalGate;
import com.iptvcinema.tv.core.parental.PinHasher;
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
public final class ParentalControlsViewModel_Factory implements Factory<ParentalControlsViewModel> {
  private final Provider<ProfilesRepository> profilesRepositoryProvider;

  private final Provider<ParentalControlsRepository> parentalControlsRepositoryProvider;

  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<PinHasher> pinHasherProvider;

  private final Provider<ParentalGate> parentalGateProvider;

  public ParentalControlsViewModel_Factory(Provider<ProfilesRepository> profilesRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider, Provider<PinHasher> pinHasherProvider,
      Provider<ParentalGate> parentalGateProvider) {
    this.profilesRepositoryProvider = profilesRepositoryProvider;
    this.parentalControlsRepositoryProvider = parentalControlsRepositoryProvider;
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.pinHasherProvider = pinHasherProvider;
    this.parentalGateProvider = parentalGateProvider;
  }

  @Override
  public ParentalControlsViewModel get() {
    return newInstance(profilesRepositoryProvider.get(), parentalControlsRepositoryProvider.get(), catalogRepositoryProvider.get(), pinHasherProvider.get(), parentalGateProvider.get());
  }

  public static ParentalControlsViewModel_Factory create(
      javax.inject.Provider<ProfilesRepository> profilesRepositoryProvider,
      javax.inject.Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<PinHasher> pinHasherProvider,
      javax.inject.Provider<ParentalGate> parentalGateProvider) {
    return new ParentalControlsViewModel_Factory(Providers.asDaggerProvider(profilesRepositoryProvider), Providers.asDaggerProvider(parentalControlsRepositoryProvider), Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(pinHasherProvider), Providers.asDaggerProvider(parentalGateProvider));
  }

  public static ParentalControlsViewModel_Factory create(
      Provider<ProfilesRepository> profilesRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider, Provider<PinHasher> pinHasherProvider,
      Provider<ParentalGate> parentalGateProvider) {
    return new ParentalControlsViewModel_Factory(profilesRepositoryProvider, parentalControlsRepositoryProvider, catalogRepositoryProvider, pinHasherProvider, parentalGateProvider);
  }

  public static ParentalControlsViewModel newInstance(ProfilesRepository profilesRepository,
      ParentalControlsRepository parentalControlsRepository, CatalogRepository catalogRepository,
      PinHasher pinHasher, ParentalGate parentalGate) {
    return new ParentalControlsViewModel(profilesRepository, parentalControlsRepository, catalogRepository, pinHasher, parentalGate);
  }
}
