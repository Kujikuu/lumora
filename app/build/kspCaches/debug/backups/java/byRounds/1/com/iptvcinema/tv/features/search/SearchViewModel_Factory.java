package com.iptvcinema.tv.features.search;

import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.data.repository.ParentalControlsRepository;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import com.iptvcinema.tv.core.datastore.RecentSearchRepository;
import com.iptvcinema.tv.core.parental.ParentalGate;
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
public final class SearchViewModel_Factory implements Factory<SearchViewModel> {
  private final Provider<CatalogRepository> catalogRepositoryProvider;

  private final Provider<RecentSearchRepository> recentSearchRepositoryProvider;

  private final Provider<AppSessionRepository> appSessionRepositoryProvider;

  private final Provider<ParentalControlsRepository> parentalControlsRepositoryProvider;

  private final Provider<ParentalGate> parentalGateProvider;

  private final Provider<AppStrings> appStringsProvider;

  public SearchViewModel_Factory(Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<RecentSearchRepository> recentSearchRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider, Provider<AppStrings> appStringsProvider) {
    this.catalogRepositoryProvider = catalogRepositoryProvider;
    this.recentSearchRepositoryProvider = recentSearchRepositoryProvider;
    this.appSessionRepositoryProvider = appSessionRepositoryProvider;
    this.parentalControlsRepositoryProvider = parentalControlsRepositoryProvider;
    this.parentalGateProvider = parentalGateProvider;
    this.appStringsProvider = appStringsProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(catalogRepositoryProvider.get(), recentSearchRepositoryProvider.get(), appSessionRepositoryProvider.get(), parentalControlsRepositoryProvider.get(), parentalGateProvider.get(), appStringsProvider.get());
  }

  public static SearchViewModel_Factory create(
      javax.inject.Provider<CatalogRepository> catalogRepositoryProvider,
      javax.inject.Provider<RecentSearchRepository> recentSearchRepositoryProvider,
      javax.inject.Provider<AppSessionRepository> appSessionRepositoryProvider,
      javax.inject.Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      javax.inject.Provider<ParentalGate> parentalGateProvider,
      javax.inject.Provider<AppStrings> appStringsProvider) {
    return new SearchViewModel_Factory(Providers.asDaggerProvider(catalogRepositoryProvider), Providers.asDaggerProvider(recentSearchRepositoryProvider), Providers.asDaggerProvider(appSessionRepositoryProvider), Providers.asDaggerProvider(parentalControlsRepositoryProvider), Providers.asDaggerProvider(parentalGateProvider), Providers.asDaggerProvider(appStringsProvider));
  }

  public static SearchViewModel_Factory create(
      Provider<CatalogRepository> catalogRepositoryProvider,
      Provider<RecentSearchRepository> recentSearchRepositoryProvider,
      Provider<AppSessionRepository> appSessionRepositoryProvider,
      Provider<ParentalControlsRepository> parentalControlsRepositoryProvider,
      Provider<ParentalGate> parentalGateProvider, Provider<AppStrings> appStringsProvider) {
    return new SearchViewModel_Factory(catalogRepositoryProvider, recentSearchRepositoryProvider, appSessionRepositoryProvider, parentalControlsRepositoryProvider, parentalGateProvider, appStringsProvider);
  }

  public static SearchViewModel newInstance(CatalogRepository catalogRepository,
      RecentSearchRepository recentSearchRepository, AppSessionRepository appSessionRepository,
      ParentalControlsRepository parentalControlsRepository, ParentalGate parentalGate,
      AppStrings appStrings) {
    return new SearchViewModel(catalogRepository, recentSearchRepository, appSessionRepository, parentalControlsRepository, parentalGate, appStrings);
  }
}
