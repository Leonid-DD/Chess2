buildscript {
    dependencies {
        classpath(libs.google.services)
        classpath(libs.kotlin.gradle.plugin)
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false

    id("com.google.gms.google-services") version "4.4.1" apply false
}