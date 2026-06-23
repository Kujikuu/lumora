package com.iptvcinema.tv.core.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class CoroutineModule_ProvideApplicationScopeFactory implements Factory<CoroutineScope> {
  @Override
  public CoroutineScope get() {
    return provideApplicationScope();
  }

  public static CoroutineModule_ProvideApplicationScopeFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CoroutineScope provideApplicationScope() {
    return Preconditions.checkNotNullFromProvides(CoroutineModule.INSTANCE.provideApplicationScope());
  }

  private static final class InstanceHolder {
    static final CoroutineModule_ProvideApplicationScopeFactory INSTANCE = new CoroutineModule_ProvideApplicationScopeFactory();
  }
}
