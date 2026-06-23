package com.iptvcinema.tv.core.player;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class PlaybackSessionTracker_Factory implements Factory<PlaybackSessionTracker> {
  @Override
  public PlaybackSessionTracker get() {
    return newInstance();
  }

  public static PlaybackSessionTracker_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PlaybackSessionTracker newInstance() {
    return new PlaybackSessionTracker();
  }

  private static final class InstanceHolder {
    static final PlaybackSessionTracker_Factory INSTANCE = new PlaybackSessionTracker_Factory();
  }
}
