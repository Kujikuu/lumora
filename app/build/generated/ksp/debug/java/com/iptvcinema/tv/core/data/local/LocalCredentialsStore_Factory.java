package com.iptvcinema.tv.core.data.local;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;

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
public final class LocalCredentialsStore_Factory implements Factory<LocalCredentialsStore> {
  private final Provider<Context> contextProvider;

  private final Provider<Json> jsonProvider;

  public LocalCredentialsStore_Factory(Provider<Context> contextProvider,
      Provider<Json> jsonProvider) {
    this.contextProvider = contextProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public LocalCredentialsStore get() {
    return newInstance(contextProvider.get(), jsonProvider.get());
  }

  public static LocalCredentialsStore_Factory create(javax.inject.Provider<Context> contextProvider,
      javax.inject.Provider<Json> jsonProvider) {
    return new LocalCredentialsStore_Factory(Providers.asDaggerProvider(contextProvider), Providers.asDaggerProvider(jsonProvider));
  }

  public static LocalCredentialsStore_Factory create(Provider<Context> contextProvider,
      Provider<Json> jsonProvider) {
    return new LocalCredentialsStore_Factory(contextProvider, jsonProvider);
  }

  public static LocalCredentialsStore newInstance(Context context, Json json) {
    return new LocalCredentialsStore(context, json);
  }
}
