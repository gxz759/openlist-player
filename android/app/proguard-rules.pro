# Add project specific ProGuard rules here.
-keep class com.example.contentplayer.data.** { *; }
-keep class com.example.contentplayer.ui.** { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
