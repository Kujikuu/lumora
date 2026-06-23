package com.iptvcinema.tv.app;

import coil.ImageLoader;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class IptvCinemaApp_MembersInjector implements MembersInjector<IptvCinemaApp> {
  private final Provider<ImageLoader> imageLoaderProvider;

  public IptvCinemaApp_MembersInjector(Provider<ImageLoader> imageLoaderProvider) {
    this.imageLoaderProvider = imageLoaderProvider;
  }

  public static MembersInjector<IptvCinemaApp> create(Provider<ImageLoader> imageLoaderProvider) {
    return new IptvCinemaApp_MembersInjector(imageLoaderProvider);
  }

  public static MembersInjector<IptvCinemaApp> create(
      javax.inject.Provider<ImageLoader> imageLoaderProvider) {
    return new IptvCinemaApp_MembersInjector(Providers.asDaggerProvider(imageLoaderProvider));
  }

  @Override
  public void injectMembers(IptvCinemaApp instance) {
    injectImageLoader(instance, imageLoaderProvider.get());
  }

  @InjectedFieldSignature("com.iptvcinema.tv.app.IptvCinemaApp.imageLoader")
  public static void injectImageLoader(IptvCinemaApp instance, ImageLoader imageLoader) {
    instance.imageLoader = imageLoader;
  }
}
