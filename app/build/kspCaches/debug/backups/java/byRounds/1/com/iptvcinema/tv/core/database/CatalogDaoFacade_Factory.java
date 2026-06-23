package com.iptvcinema.tv.core.database;

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
public final class CatalogDaoFacade_Factory implements Factory<CatalogDaoFacade> {
  private final Provider<IptvDatabase> databaseProvider;

  public CatalogDaoFacade_Factory(Provider<IptvDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public CatalogDaoFacade get() {
    return newInstance(databaseProvider.get());
  }

  public static CatalogDaoFacade_Factory create(
      javax.inject.Provider<IptvDatabase> databaseProvider) {
    return new CatalogDaoFacade_Factory(Providers.asDaggerProvider(databaseProvider));
  }

  public static CatalogDaoFacade_Factory create(Provider<IptvDatabase> databaseProvider) {
    return new CatalogDaoFacade_Factory(databaseProvider);
  }

  public static CatalogDaoFacade newInstance(IptvDatabase database) {
    return new CatalogDaoFacade(database);
  }
}
