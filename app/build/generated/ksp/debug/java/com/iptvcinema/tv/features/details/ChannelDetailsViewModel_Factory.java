package com.iptvcinema.tv.features.details;

import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.data.repository.FavoritesRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import com.iptvcinema.tv.core.util.AppStrings;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class ChannelDetailsViewModel_Factory implements Factory<ChannelDetailsViewModel> {
  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<FavoritesRepository> favoritesRepositoryProvider;

  private final Provider<AppStrings> appStringsProvider;

  public ChannelDetailsViewModel_Factory(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<AppStrings> appStringsProvider) {
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.favoritesRepositoryProvider = favoritesRepositoryProvider;
    this.appStringsProvider = appStringsProvider;
  }

  @Override
  public ChannelDetailsViewModel get() {
    return newInstance(appSessionRepositoryProvider.get(), catalogRepositoryProvider.get(), favoritesRepositoryProvider.get(), appStringsProvider.get());
  }

  public static ChannelDetailsViewModel_Factory create(
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<FavoritesRepository> favoritesRepositoryProvider,
      javax.inject.Provider<AppStrings> appStringsProvider) {
    return new ChannelDetailsViewModel_Factory(Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(favoritesRepositoryProvider), Providers.asDaggerProvider(appStringsProvider));
  }

  public static ChannelDetailsViewModel_Factory create(
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<FavoritesRepository> favoritesRepositoryProvider,
      Provider<AppStrings> appStringsProvider) {
    return new ChannelDetailsViewModel_Factory(appSessionRepositoryProvider, catalogRepositoryProvider, favoritesRepositoryProvider, appStringsProvider);
  }

  public static ChannelDetailsViewModel newInstance(AppSessionRepository appSessionRepository,
      CatalogRepository catalogRepository, FavoritesRepository favoritesRepository,
      AppStrings appStrings) {
    return new ChannelDetailsViewModel(appSessionRepository, catalogRepository, favoritesRepository, appStrings);
  }
}
