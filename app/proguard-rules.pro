# Add entries to help debug native memory problems
# -XX:NativeMemoryTracking=summary

# Google Play Services & Maps
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }

# Google API Client for Android (Drive API)
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
-dontwarn com.google.api.client.**
-dontwarn com.google.common.**

# Firebase
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }
