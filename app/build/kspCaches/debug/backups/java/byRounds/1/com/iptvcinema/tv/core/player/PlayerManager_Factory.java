package com.iptvcinema.tv.core.player;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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

  public PlayerManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public PlayerManager get() {
    return newInstance(contextProvider.get());
  }

  public static PlayerManager_Factory create(javax.inject.Provider<Context> contextProvider) {
    return new PlayerManager_Factory(Providers.asDaggerProvider(contextProvider));
  }

  public static PlayerManager_Factory create(Provider<Context> contextProvider) {
    return new PlayerManager_Factory(contextProvider);
  }

  public static PlayerManager newInstance(Context context) {
    return new PlayerManager(context);
  }
}
