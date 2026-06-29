package com.iptvcinema.tv.core.player;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "dagger.hilt.android.qualifiers.ApplicationContext",
    "com.iptvcinema.tv.core.di.ApplicationScope"
})
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
public final class PlayerManager_Factory implements Factory<PlayerManager> {
  private final Provider<Context> contextProvider;

  private final Provider<CoroutineScope> applicationScopeProvider;

  public PlayerManager_Factory(Provider<Context> contextProvider,
      Provider<CoroutineScope> applicationScopeProvider) {
    this.contextProvider = contextProvider;
    this.applicationScopeProvider = applicationScopeProvider;
  }

  @Override
  public PlayerManager get() {
    return newInstance(contextProvider.get(), applicationScopeProvider.get());
  }

  public static PlayerManager_Factory create(javax.inject.Provider<Context> contextProvider,
      javax.inject.Provider<CoroutineScope> applicationScopeProvider) {
    return new PlayerManager_Factory(Providers.asDaggerProvider(contextProvider), Providers.asDaggerProvider(applicationScopeProvider));
  }

  public static PlayerManager_Factory create(Provider<Context> contextProvider,
      Provider<CoroutineScope> applicationScopeProvider) {
    return new PlayerManager_Factory(contextProvider, applicationScopeProvider);
  }

  public static PlayerManager newInstance(Context context, CoroutineScope applicationScope) {
    return new PlayerManager(context, applicationScope);
  }
}
