package com.iptvcinema.tv.core.epg;

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
public final class XmltvParser_Factory implements Factory<XmltvParser> {
  @Override
  public XmltvParser get() {
    return newInstance();
  }

  public static XmltvParser_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static XmltvParser newInstance() {
    return new XmltvParser();
  }

  private static final class InstanceHolder {
    static final XmltvParser_Factory INSTANCE = new XmltvParser_Factory();
  }
}
