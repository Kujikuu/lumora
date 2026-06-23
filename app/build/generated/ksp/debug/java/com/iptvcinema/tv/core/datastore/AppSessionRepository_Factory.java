package com.iptvcinema.tv.core.datastore;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
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
public final class AppSessionRepository_Factory implements Factory<AppSessionRepository> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public AppSessionRepository_Factory(Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public AppSessionRepository get() {
    return newInstance(dataStoreProvider.get());
  }

  public static AppSessionRepository_Factory create(
      javax.inject.Provider<DataStore<Preferences>> dataStoreProvider) {
    return new AppSessionRepository_Factory(Providers.asDaggerProvider(dataStoreProvider));
  }

  public static AppSessionRepository_Factory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new AppSessionRepository_Factory(dataStoreProvider);
  }

  public static AppSessionRepository newInstance(DataStore<Preferences> dataStore) {
    return new AppSessionRepository(dataStore);
  }
}
