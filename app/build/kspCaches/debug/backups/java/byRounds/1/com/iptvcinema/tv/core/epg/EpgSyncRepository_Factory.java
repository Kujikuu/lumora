package com.iptvcinema.tv.core.epg;

import com.iptvcinema.tv.core.database.CatalogDaoFacade;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.iptvcinema.tv.core.di.ApplicationScope")
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
public final class EpgSyncRepository_Factory implements Factory<EpgSyncRepository> {
  private final Provider<CatalogDaoFacade> catalogDaoFacadeProvider;

  private final Provider<XmltvParser> xmltvParserProvider;

  private final Provider<CoroutineScope> applicationScopeProvider;

  public EpgSyncRepository_Factory(Provider<CatalogDaoFacade> catalogDaoFacadeProvider,
      Provider<XmltvParser> xmltvParserProvider,
      Provider<CoroutineScope> applicationScopeProvider) {
    this.catalogDaoFacadeProvider = catalogDaoFacadeProvider;
    this.xmltvParserProvider = xmltvParserProvider;
    this.applicationScopeProvider = applicationScopeProvider;
  }

  @Override
  public EpgSyncRepository get() {
    return newInstance(catalogDaoFacadeProvider.get(), xmltvParserProvider.get(), applicationScopeProvider.get());
  }

  public static EpgSyncRepository_Factory create(
      javax.inject.Provider<CatalogDaoFacade> catalogDaoFacadeProvider,
      javax.inject.Provider<XmltvParser> xmltvParserProvider,
      javax.inject.Provider<CoroutineScope> applicationScopeProvider) {
    return new EpgSyncRepository_Factory(Providers.asDaggerProvider(catalogDaoFacadeProvider), Providers.asDaggerProvider(xmltvParserProvider), Providers.asDaggerProvider(applicationScopeProvider));
  }

  public static EpgSyncRepository_Factory create(
      Provider<CatalogDaoFacade> catalogDaoFacadeProvider,
      Provider<XmltvParser> xmltvParserProvider,
      Provider<CoroutineScope> applicationScopeProvider) {
    return new EpgSyncRepository_Factory(catalogDaoFacadeProvider, xmltvParserProvider, applicationScopeProvider);
  }

  public static EpgSyncRepository newInstance(CatalogDaoFacade catalogDaoFacade,
      XmltvParser xmltvParser, CoroutineScope applicationScope) {
    return new EpgSyncRepository(catalogDaoFacade, xmltvParser, applicationScope);
  }
}
