package com.iptvcinema.tv.core.data.repository.supabase;

import com.iptvcinema.tv.core.datastore.AppSessionRepository;
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
public final class SupabaseAuthRepository_Factory implements Factory<SupabaseAuthRepository> {
  private final Provider<SupabaseClient> supabaseClientProvider;

  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  public SupabaseAuthRepository_Factory(Provider<SupabaseClient> supabaseClientProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider) {
    this.supabaseClientProvider = supabaseClientProvider;
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
  }

  @Override
  public SupabaseAuthRepository get() {
    return newInstance(supabaseClientProvider.get(), appSessionRepositoryProvider.get());
  }

  public static SupabaseAuthRepository_Factory create(
      javax.inject.Provider<SupabaseClient> supabaseClientProvider,
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider) {
    return new SupabaseAuthRepository_Factory(Providers.asDaggerProvider(supabaseClientProvider), Providers.asDaggerProvider(appSessionRepositoryProvider));
  }

  public static SupabaseAuthRepository_Factory create(
      Provider<SupabaseClient> supabaseClientProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider) {
    return new SupabaseAuthRepository_Factory(supabaseClientProvider, appSessionRepositoryProvider);
  }

  public static SupabaseAuthRepository newInstance(SupabaseClient supabaseClient,
      AppSessionRepository appSessionRepository) {
    return new SupabaseAuthRepository(supabaseClient, appSessionRepository);
  }
}
