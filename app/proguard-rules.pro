# ─── Kotlin ───────────────────────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ─── Kotlin Coroutines ────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ─── Kotlinx Serialization ────────────────────────────────────────────────────
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ─── Ktor ─────────────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ─── OkHttp (Ktor Android engine) ─────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ─── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.**

# ─── Koin ─────────────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-keepnames class * implements org.koin.core.module.Module
-dontwarn org.koin.**

# ─── Voyager ──────────────────────────────────────────────────────────────────
-keep class cafe.adriel.voyager.** { *; }
-dontwarn cafe.adriel.voyager.**

# ─── Compose ──────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── Credential Manager / Google Identity ────────────────────────────────────
-keep class androidx.credentials.** { *; }
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**

# ─── App domain models ────────────────────────────────────────────────────────
# As duas regras que estavam aqui apontavam para com.walcker.bible.**, pacote
# que não existe neste projeto — resquício do Lexis, e portanto no-op.
-keep class com.walcker.games.features.domain.model.** { *; }
-keep class com.walcker.identity.features.domain.** { *; }

# ─── BuildConfig ──────────────────────────────────────────────────────────────
-keep class com.walcker.match.app.BuildConfig { *; }
