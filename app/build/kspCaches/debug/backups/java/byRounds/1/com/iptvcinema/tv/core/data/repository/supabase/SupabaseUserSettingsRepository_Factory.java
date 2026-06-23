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
public final class SupabaseUserSettingsRepository_Factory implements Factory<SupabaseUserSettingsRepository> {
  private final Provider<SupabaseClient> supabaseClientProvider;

  public SupabaseUserSettingsRepository_Factory(Provider<SupabaseClient> supabaseClientProvider) {
    this.supabaseClientProvider = supabaseClientProvider;
  }

  @Override
  public SupabaseUserSettingsRepository get() {
    return newInstance(supabaseClientProvider.get());
  }

  public static SupabaseUserSettingsRepository_Factory create(
      javax.inject.Provider<SupabaseClient> supabaseClientProvider) {
    return new SupabaseUserSettingsRepository_Factory(Providers.asDaggerProvider(supabaseClientProvider));
  }

  public static SupabaseUserSettingsRepository_Factory create(
      Provider<SupabaseClient> supabaseClientProvider) {
    return new SupabaseUserSettingsRepository_Factory(supabaseClientProvider);
  }

  public static SupabaseUserSettingsRepository newInstance(SupabaseClient supabaseClient) {
    return new SupabaseUserSettingsRepository(supabaseClient);
  }
}
