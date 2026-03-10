# Add project specific ProGuard rules here.

# ===== Keep rules for Garuda app =====

# --- App data models ---
-keep class com.example.garuda.data.repository.AudioMetadata { *; }
-keep class com.example.garuda.data.local.entity.** { *; }
-keep class com.example.garuda.domain.classifier.EmergencyResult { *; }
-keep class com.example.garuda.domain.classifier.AnalysisInput { *; }
-keep class com.example.garuda.domain.classifier.AudioToneIndicators { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# --- Retrofit + Gson ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# --- Gemini AI SDK ---
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# --- Hilt / Dagger ---
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- Kotlin Serialization (if used in future) ---
-keepattributes RuntimeVisibleAnnotations

# --- Preserve line numbers for stack traces ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile