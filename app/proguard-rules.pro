# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Firebase rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Google Play Services specific rules
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.ads.** { *; }

# Prevent obfuscation of Google Play Services classes
-keepnames class com.google.android.gms.** { *; }
-keepnames class com.google.firebase.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Firebase data classes
-keep class com.saadho.funnutv.data.Video { *; }
-keep class com.saadho.funnutv.data.VideoResponse { *; }

# Keep Firebase initialization
-keep class com.saadho.funnutv.funnuTVApp { *; }