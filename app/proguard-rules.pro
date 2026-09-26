# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers for readable stacktraces in crash monitoring
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room database rules
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# CAD Model & Entity rules
-keep class com.example.cad.model.** { *; }
-keep class com.example.cad.data.** { *; }

# Jetpack Compose rules
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**
