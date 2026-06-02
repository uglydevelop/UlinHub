// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false // Версия живет ТОЛЬКО тут!
    id("com.google.devtools.ksp") version "2.0.0-1.0.22" // Версия должна соответствовать твоему Kotlin
}