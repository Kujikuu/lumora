package com.iptvcinema.tv.core.util;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppStrings_Factory implements Factory<AppStrings> {
  private final Provider<Context> contextProvider;

  public AppStrings_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AppStrings get() {
    return newInstance(contextProvider.get());
  }

  public static AppStrings_Factory create(javax.inject.Provider<Context> contextProvider) {
    return new AppStrings_Factory(Providers.asDaggerProvider(contextProvider));
  }

  public static AppStrings_Factory create(Provider<Context> contextProvider) {
    return new AppStrings_Factory(contextProvider);
  }

  public static AppStrings newInstance(Context context) {
    return new AppStrings(context);
  }
}
