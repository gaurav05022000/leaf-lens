-keep class com.example.api.** { *; }
-keepclassmembers class com.example.api.** {
    <fields>;
    <init>(...);
}

-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonQualifier interface *

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature, Exceptions, *Annotation*
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Room
-keep class com.example.data.room.** { *; }
-keepclassmembers class com.example.data.room.** {
    <fields>;
    <init>(...);
}

# Firestore Data Classes
-keep class com.example.ui.PointsManager$PointTransaction { *; }
-keepclassmembers class com.example.ui.PointsManager$PointTransaction { *; }

-keep class com.example.ui.ScanHistoryItem { *; }
-keepclassmembers class com.example.ui.ScanHistoryItem { *; }

-keep class com.example.ui.ChatMessage { *; }
-keepclassmembers class com.example.ui.ChatMessage { *; }

-keep class com.example.data.Plant { *; }
-keepclassmembers class com.example.data.Plant { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Androidx
-keep class androidx.lifecycle.** { *; }
-keep class com.google.android.gms.internal.location.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keep class kotlin.jvm.internal.** { *; }
-keep class com.example.worker.** { *; }
