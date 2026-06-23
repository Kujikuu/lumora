package com.iptvcinema.tv.core.data.repository.supabase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
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
public final class SupabaseFavoritesRepository_Factory implements Factory<SupabaseFavoritesRepository> {
  private final Provider<SupabaseClient> supabaseClientProvider;

  public SupabaseFavoritesRepository_Factory(Provider<SupabaseClient> supabaseClientProvider) {
    this.supabaseClientProvider = supabaseClientProvider;
  }

  @Override
  public SupabaseFavoritesRepository get() {
    return newInstance(supabaseClientProvider.get());
  }

  public static SupabaseFavoritesRepository_Factory create(
      javax.inject.Provider<SupabaseClient> supabaseClientProvider) {
    return new SupabaseFavoritesRepository_Factory(Providers.asDaggerProvider(supabaseClientProvider));
  }

  public static SupabaseFavoritesRepository_Factory create(
      Provider<SupabaseClient> supabaseClientProvider) {
    return new SupabaseFavoritesRepository_Factory(supabaseClientProvider);
  }

  public static SupabaseFavoritesRepository newInstance(SupabaseClient supabaseClient) {
    return new SupabaseFavoritesRepository(supabaseClient);
  }
}
