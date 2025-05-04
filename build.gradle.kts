// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:...") // Keep existing AGP version
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:...") // Keep existing Kotlin plugin version
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51.1") // Add Hilt plugin classpath
    }
}