package com.iptvcinema.tv.core.data.repository.supabase;

import com.iptvcinema.tv.core.data.local.LocalCredentialsStore;
import com.iptvcinema.tv.core.supabase.security.CloudCredentialsCipher;
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
public final class SupabasePlaylistSourcesRepository_Factory implements Factory<SupabasePlaylistSourcesRepository> {
  private final Provider<SupabaseClient> supabaseClientProvider;

  private final Provider<LocalCredentialsStore> localCredentialsStoreProvider;

  private final Provider<CloudCredentialsCipher> cloudCredentialsCipherProvider;

  public SupabasePlaylistSourcesRepository_Factory(Provider<SupabaseClient> supabaseClientProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      Provider<CloudCredentialsCipher> cloudCredentialsCipherProvider) {
    this.supabaseClientProvider = supabaseClientProvider;
    this.localCredentialsStoreProvider = localCredentialsStoreProvider;
    this.cloudCredentialsCipherProvider = cloudCredentialsCipherProvider;
  }

  @Override
  public SupabasePlaylistSourcesRepository get() {
    return newInstance(supabaseClientProvider.get(), localCredentialsStoreProvider.get(), cloudCredentialsCipherProvider.get());
  }

  public static SupabasePlaylistSourcesRepository_Factory create(
      javax.inject.Provider<SupabaseClient> supabaseClientProvider,
      javax.inject.Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      javax.inject.Provider<CloudCredentialsCipher> cloudCredentialsCipherProvider) {
    return new SupabasePlaylistSourcesRepository_Factory(Providers.asDaggerProvider(supabaseClientProvider), Providers.asDaggerProvider(localCredentialsStoreProvider), Providers.asDaggerProvider(cloudCredentialsCipherProvider));
  }

  public static SupabasePlaylistSourcesRepository_Factory create(
      Provider<SupabaseClient> supabaseClientProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider,
      Provider<CloudCredentialsCipher> cloudCredentialsCipherProvider) {
    return new SupabasePlaylistSourcesRepository_Factory(supabaseClientProvider, localCredentialsStoreProvider, cloudCredentialsCipherProvider);
  }

  public static SupabasePlaylistSourcesRepository newInstance(SupabaseClient supabaseClient,
      LocalCredentialsStore localCredentialsStore, CloudCredentialsCipher cloudCredentialsCipher) {
    return new SupabasePlaylistSourcesRepository(supabaseClient, localCredentialsStore, cloudCredentialsCipher);
  }
}
