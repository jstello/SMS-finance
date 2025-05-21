// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    // alias(libs.plugins.google.gms.google.services) apply false // Removed as part of Firebase to Room migration
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}

// buildscript block can be removed if Hilt was the only item not covered by plugins block or version catalog
// If AGP and Kotlin plugin classpaths were explicitly defined here and are still needed,
// they should also be migrated or ensured they are correctly managed by version catalog or other means.
// For this change, assuming they are managed elsewhere or were placeholders.