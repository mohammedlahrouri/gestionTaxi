# ==============================================================================
# ProGuard / R8 Optimization & Minification Rules - Taxi Management
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Depuración y Trazabilidad (Stacktraces legibles con mapping.txt)
# ------------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# 2. Modelos de Datos Locales y DTOs (Preservar nombres y campos)
# ------------------------------------------------------------------------------
-keep class com.moham.taxi.data.model.** { *; }
-keep class com.moham.taxi.BillingData { *; }

# ------------------------------------------------------------------------------
# 3. Room Database
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# 4. WorkManager (Background Workers instanciados por reflexión)
# ------------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.moham.taxi.data.online.FirebaseSyncWorker { *; }
-keep class com.moham.taxi.data.online.OnlineBackupWorker { *; }

# ------------------------------------------------------------------------------
# 5. iText 7 PDF (Generación de presupuestos y reportes)
# ------------------------------------------------------------------------------
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
-dontwarn org.bouncycastle.**

# ------------------------------------------------------------------------------
# 6. Google API Client & Google Drive REST API (Backup Online)
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.client.json.gson.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.common.**
-dontwarn org.apache.http.**
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**

# ------------------------------------------------------------------------------
# 7. Google Play Billing
# ------------------------------------------------------------------------------
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.**

# ------------------------------------------------------------------------------
# 8. Firebase (Firestore, Auth, Storage)
# ------------------------------------------------------------------------------
-keepattributes *Annotation*
-dontwarn com.google.firebase.**

# ------------------------------------------------------------------------------
# 9. Kotlin Coroutines & Jetpack DataStore
# ------------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class androidx.datastore.** { *; }

# ------------------------------------------------------------------------------
# 10. Logging (SLF4J / Apache Commons Logging - Opcionales en iText 7 y Google API)
# ------------------------------------------------------------------------------
-dontwarn org.slf4j.**
-dontwarn org.apache.commons.logging.**