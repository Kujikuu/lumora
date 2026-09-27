# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Serialized DTOs: kotlinx.serialization ships rules for generated serializers, but these
# classes are also read by name through Supabase/Postgrest column mapping. Keep them intact.
-keep @kotlinx.serialization.Serializable class com.iptvcinema.tv.** { *; }
-keepclassmembers class com.iptvcinema.tv.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep line numbers so release crash reports stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
