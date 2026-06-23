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
public final class ParentalSession_Factory implements Factory<ParentalSession> {
  @Override
  public ParentalSession get() {
    return newInstance();
  }

  public static ParentalSession_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ParentalSession newInstance() {
    return new ParentalSession();
  }

  private static final class InstanceHolder {
    static final ParentalSession_Factory INSTANCE = new ParentalSession_Factory();
  }
}
