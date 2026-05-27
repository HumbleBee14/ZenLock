# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ============================================================================
# SECURITY: Remove all debug logs in release builds
# ============================================================================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Remove System.out.print statements
-assumenosideeffects class java.io.PrintStream {
    public void println(%);
    public void println(**);
    public void print(%);
    public void print(**);
}

# ============================================================================
# SECURITY: Obfuscate sensitive classes
# ============================================================================
-keep class com.grepguru.zenlock.utils.OTPManager {
    # Keep only essential methods, obfuscate internals
    public boolean verifyOTP(...);
    public void sendOTP(...);
}

# ============================================================================
# SECURITY: Keep line numbers for crash reports but hide source files
# ============================================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================================
# ROOM DATABASE: Keep Room classes and entities
# ============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class com.grepguru.zenlock.data.entities.** { *; }
-keep class com.grepguru.zenlock.data.dao.** { *; }
-keep class com.grepguru.zenlock.data.database.** { *; }

# Keep Room TypeConverters
-keep class * extends androidx.room.TypeConverter
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase$Callback
-keep class * extends androidx.room.RoomDatabase$Migration

# ============================================================================
# SCHEDULE MODEL: Keep schedule-related classes
# ============================================================================
-keep class com.grepguru.zenlock.model.ScheduleModel { *; }
-keep class com.grepguru.zenlock.model.ScheduleModel$RepeatType { *; }
-keep class com.grepguru.zenlock.utils.ScheduleManager { *; }

# ============================================================================
# PERFORMANCE: Additional optimizations
# ============================================================================
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}