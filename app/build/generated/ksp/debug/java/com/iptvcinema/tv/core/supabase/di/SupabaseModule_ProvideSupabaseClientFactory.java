package com.iptvcinema.tv.core.supabase.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;

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
public final class SupabaseModule_ProvideSupabaseClientFactory implements Factory<SupabaseClient> {
  private final Provider<Json> jsonProvider;

  public SupabaseModule_ProvideSupabaseClientFactory(Provider<Json> jsonProvider) {
    this.jsonProvider = jsonProvider;
  }

  @Override
  public SupabaseClient get() {
    return provideSupabaseClient(jsonProvider.get());
  }

  public static SupabaseModule_ProvideSupabaseClientFactory create(
      javax.inject.Provider<Json> jsonProvider) {
    return new SupabaseModule_ProvideSupabaseClientFactory(Providers.asDaggerProvider(jsonProvider));
  }

  public static SupabaseModule_ProvideSupabaseClientFactory create(Provider<Json> jsonProvider) {
    return new SupabaseModule_ProvideSupabaseClientFactory(jsonProvider);
  }

  public static SupabaseClient provideSupabaseClient(Json json) {
    return Preconditions.checkNotNullFromProvides(SupabaseModule.INSTANCE.provideSupabaseClient(json));
  }
}
