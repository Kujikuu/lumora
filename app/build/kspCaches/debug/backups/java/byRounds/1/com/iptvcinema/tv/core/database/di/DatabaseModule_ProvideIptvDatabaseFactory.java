package com.iptvcinema.tv.core.database.di;

import android.content.Context;
import com.iptvcinema.tv.core.database.IptvDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideIptvDatabaseFactory implements Factory<IptvDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideIptvDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public IptvDatabase get() {
    return provideIptvDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideIptvDatabaseFactory create(
      javax.inject.Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideIptvDatabaseFactory(Providers.asDaggerProvider(contextProvider));
  }

  public static DatabaseModule_ProvideIptvDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideIptvDatabaseFactory(contextProvider);
  }

  public static IptvDatabase provideIptvDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideIptvDatabase(context));
  }
}
