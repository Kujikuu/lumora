package com.iptvcinema.tv.core.xtream;

import com.iptvcinema.tv.core.data.local.LocalCredentialsStore;
import com.iptvcinema.tv.core.network.XtreamRetrofitFactory;
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
public final class XtreamRepository_Factory implements Factory<XtreamRepository> {
  private final Provider<XtreamRetrofitFactory> retrofitFactoryProvider;

  private final Provider<LocalCredentialsStore> localCredentialsStoreProvider;

  public XtreamRepository_Factory(Provider<XtreamRetrofitFactory> retrofitFactoryProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider) {
    this.retrofitFactoryProvider = retrofitFactoryProvider;
    this.localCredentialsStoreProvider = localCredentialsStoreProvider;
  }

  @Override
  public XtreamRepository get() {
    return newInstance(retrofitFactoryProvider.get(), localCredentialsStoreProvider.get());
  }

  public static XtreamRepository_Factory create(
      javax.inject.Provider<XtreamRetrofitFactory> retrofitFactoryProvider,
      javax.inject.Provider<LocalCredentialsStore> localCredentialsStoreProvider) {
    return new XtreamRepository_Factory(Providers.asDaggerProvider(retrofitFactoryProvider), Providers.asDaggerProvider(localCredentialsStoreProvider));
  }

  public static XtreamRepository_Factory create(
      Provider<XtreamRetrofitFactory> retrofitFactoryProvider,
      Provider<LocalCredentialsStore> localCredentialsStoreProvider) {
    return new XtreamRepository_Factory(retrofitFactoryProvider, localCredentialsStoreProvider);
  }

  public static XtreamRepository newInstance(XtreamRetrofitFactory retrofitFactory,
      LocalCredentialsStore localCredentialsStore) {
    return new XtreamRepository(retrofitFactory, localCredentialsStore);
  }
}
