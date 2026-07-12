-keep class com.meteory.optimizer.** { *; }
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
