plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    // Firebase / Google Services. Applied in app/build.gradle.kts only when
    // app/google-services.json is present, so the repo builds without secrets.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
