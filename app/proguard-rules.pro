# Add project specific ProGuard rules here.
#
# Release builds run R8 with shrinking and obfuscation enabled. Most libraries
# used here (Room, Retrofit, OkHttp, Coil, Firebase) ship their own consumer
# rules, so this file only covers what is specific to this app.

# Keep line numbers and map source file names so crash reports remain readable
# after obfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Data model classes are serialised by Moshi via generated adapters and read by
# Room. Keep the classes and their members so reflection-based paths and the
# generated adapters both resolve.
-keep class com.example.data.model.** { *; }

# Moshi generated adapters are looked up by name at runtime.
-keep class **JsonAdapter { *; }
-keepnames class com.squareup.moshi.internal.NullSafeJsonAdapter

# Kotlin enum entries are referenced by name through Room type converters
# (TransportType, VerificationStatus, PlaceCategory).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep annotation and generic signature metadata that Moshi and Retrofit rely on
# to resolve parameterised types.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# OkHttp references optional platform classes that are absent on Android.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

