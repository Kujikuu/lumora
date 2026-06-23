package com.iptvcinema.tv.core.player;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class NextEpisodeResolver_Factory implements Factory<NextEpisodeResolver> {
  private final Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider;

  public NextEpisodeResolver_Factory(
      Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider) {
    this.episodeCatalogRepositoryProvider = episodeCatalogRepositoryProvider;
  }

  @Override
  public NextEpisodeResolver get() {
    return newInstance(episodeCatalogRepositoryProvider.get());
  }

  public static NextEpisodeResolver_Factory create(
      javax.inject.Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider) {
    return new NextEpisodeResolver_Factory(Providers.asDaggerProvider(episodeCatalogRepositoryProvider));
  }

  public static NextEpisodeResolver_Factory create(
      Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider) {
    return new NextEpisodeResolver_Factory(episodeCatalogRepositoryProvider);
  }

  public static NextEpisodeResolver newInstance(EpisodeCatalogRepository episodeCatalogRepository) {
    return new NextEpisodeResolver(episodeCatalogRepository);
  }
}
