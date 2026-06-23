package com.iptvcinema.tv.core.m3u;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

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
public final class M3uDownloader_Factory implements Factory<M3uDownloader> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public M3uDownloader_Factory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public M3uDownloader get() {
    return newInstance(okHttpClientProvider.get());
  }

  public static M3uDownloader_Factory create(
      javax.inject.Provider<OkHttpClient> okHttpClientProvider) {
    return new M3uDownloader_Factory(Providers.asDaggerProvider(okHttpClientProvider));
  }

  public static M3uDownloader_Factory create(Provider<OkHttpClient> okHttpClientProvider) {
    return new M3uDownloader_Factory(okHttpClientProvider);
  }

  public static M3uDownloader newInstance(OkHttpClient okHttpClient) {
    return new M3uDownloader(okHttpClient);
  }
}
