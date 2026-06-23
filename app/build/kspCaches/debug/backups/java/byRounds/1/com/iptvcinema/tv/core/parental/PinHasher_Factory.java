package com.iptvcinema.tv.core.parental;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class PinHasher_Factory implements Factory<PinHasher> {
  @Override
  public PinHasher get() {
    return newInstance();
  }

  public static PinHasher_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PinHasher newInstance() {
    return new PinHasher();
  }

  private static final class InstanceHolder {
    static final PinHasher_Factory INSTANCE = new PinHasher_Factory();
  }
}
