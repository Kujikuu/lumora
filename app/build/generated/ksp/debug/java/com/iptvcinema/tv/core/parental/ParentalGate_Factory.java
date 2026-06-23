package com.iptvcinema.tv.core.parental;

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
public final class ParentalGate_Factory implements Factory<ParentalGate> {
  private final Provider<PinHasher> pinHasherProvider;

  private final Provider<ParentalSession> parentalSessionProvider;

  public ParentalGate_Factory(Provider<PinHasher> pinHasherProvider,
      Provider<ParentalSession> parentalSessionProvider) {
    this.pinHasherProvider = pinHasherProvider;
    this.parentalSessionProvider = parentalSessionProvider;
  }

  @Override
  public ParentalGate get() {
    return newInstance(pinHasherProvider.get(), parentalSessionProvider.get());
  }

  public static ParentalGate_Factory create(javax.inject.Provider<PinHasher> pinHasherProvider,
      javax.inject.Provider<ParentalSession> parentalSessionProvider) {
    return new ParentalGate_Factory(Providers.asDaggerProvider(pinHasherProvider), Providers.asDaggerProvider(parentalSessionProvider));
  }

  public static ParentalGate_Factory create(Provider<PinHasher> pinHasherProvider,
      Provider<ParentalSession> parentalSessionProvider) {
    return new ParentalGate_Factory(pinHasherProvider, parentalSessionProvider);
  }

  public static ParentalGate newInstance(PinHasher pinHasher, ParentalSession parentalSession) {
    return new ParentalGate(pinHasher, parentalSession);
  }
}
