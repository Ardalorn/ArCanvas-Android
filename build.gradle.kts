plugins {
    id("com.android.application") version "9.5.3" apply false
    // Not applied in the app module (AGP 9 has built-in Kotlin). Declared here so the Kotlin
    // Gradle plugin on the build classpath is 2.4.21 rather than the older one AGP bundles.
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}
