package com.iptvcinema.tv.core.data.repository.supabase;

import com.iptvcinema.tv.core.data.repository.AuthRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import io.ktor.client.HttpClient;
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
public final class SupabaseDeviceActivationRepository_Factory implements Factory<SupabaseDeviceActivationRepository> {
  private final Provider<SupabaseClient> supabaseClientProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<HttpClient> httpClientProvider;

  private final Provider<Json> jsonProvider;

  public SupabaseDeviceActivationRepository_Factory(Provider<SupabaseClient> supabaseClientProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<HttpClient> httpClientProvider, Provider<Json> jsonProvider) {
    this.supabaseClientProvider = supabaseClientProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.httpClientProvider = httpClientProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public SupabaseDeviceActivationRepository get() {
    return newInstance(supabaseClientProvider.get(), authRepositoryProvider.get(), appSessionRepositoryProvider.get(), httpClientProvider.get(), jsonProvider.get());
  }

  public static SupabaseDeviceActivationRepository_Factory create(
      javax.inject.Provider<SupabaseClient> supabaseClientProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<HttpClient> httpClientProvider,
      javax.inject.Provider<Json> jsonProvider) {
    return new SupabaseDeviceActivationRepository_Factory(Providers.asDaggerProvider(supabaseClientProvider), Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(httpClientProvider), Providers.asDaggerProvider(jsonProvider));
  }

  public static SupabaseDeviceActivationRepository_Factory create(
      Provider<SupabaseClient> supabaseClientProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<HttpClient> httpClientProvider, Provider<Json> jsonProvider) {
    return new SupabaseDeviceActivationRepository_Factory(supabaseClientProvider, authRepositoryProvider, appSessionRepositoryProvider, httpClientProvider, jsonProvider);
  }

  public static SupabaseDeviceActivationRepository newInstance(SupabaseClient supabaseClient,
      AuthRepository authRepository, AppSessionRepository appSessionRepository,
      HttpClient httpClient, Json json) {
    return new SupabaseDeviceActivationRepository(supabaseClient, authRepository, appSessionRepository, httpClient, json);
  }
}
