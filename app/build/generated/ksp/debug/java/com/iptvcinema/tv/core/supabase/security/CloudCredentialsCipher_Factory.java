package com.iptvcinema.tv.core.supabase.security;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class CloudCredentialsCipher_Factory implements Factory<CloudCredentialsCipher> {
  private final Provider<Json> jsonProvider;

  public CloudCredentialsCipher_Factory(Provider<Json> jsonProvider) {
    this.jsonProvider = jsonProvider;
  }

  @Override
  public CloudCredentialsCipher get() {
    return newInstance(jsonProvider.get());
  }

  public static CloudCredentialsCipher_Factory create(javax.inject.Provider<Json> jsonProvider) {
    return new CloudCredentialsCipher_Factory(Providers.asDaggerProvider(jsonProvider));
  }

  public static CloudCredentialsCipher_Factory create(Provider<Json> jsonProvider) {
    return new CloudCredentialsCipher_Factory(jsonProvider);
  }

  public static CloudCredentialsCipher newInstance(Json json) {
    return new CloudCredentialsCipher(json);
  }
}
