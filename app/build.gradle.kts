/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension

plugins {
    alias(libs.plugins.android.application)
    //alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    id("com.google.android.gms.oss-licenses-plugin")
}

extensions.configure<ApplicationExtension> {
    namespace = "com.drewcodesit.afiexplorer"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.drewstephenscoding.afiexplorer"
        minSdk = 24
        targetSdk = 37

        // 🤖 CI/CD Versioning
        val baseCode = 42
        val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0

        versionCode = baseCode + runNumber

        val patchVersion = 6 + runNumber
        versionName = "2.1.$patchVersion($runNumber)"

        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            resValue("bool", "FIREBASE_ANALYTICS_DEACTIVATED", "true")
        }

        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("bool", "FIREBASE_ANALYTICS_DEACTIVATED", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        resValues = true
    }

    sourceSets {
        getByName("androidTest"){
            assets{
                directories.add(file("$projectDir/schemas").toString())
            }
        }
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
@Suppress("UnstableApiUsage")
extensions.configure<ApplicationAndroidComponentsExtension>("androidComponents") {
    onVariants { variant ->

        val buildType = variant.buildType

        variant.outputs.forEach { output ->

            val versionName = output.versionName.get()
            val versionCode = output.versionCode.get()

            output.outputFileName.set(
                "AFIExplorer-v$versionName($versionCode)-$buildType.apk"
            )
        }
    }
}

tasks.whenTaskAdded {
    if (name == "bundleRelease") {
        doLast {
            val outputDir = file("${layout.buildDirectory.get()}/outputs/bundle/release")

            val original = outputDir.listFiles()?.firstOrNull { it.name.endsWith(".aab") }

            if (original != null) {
                val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0
                val versionCode = 42 + runNumber
                val versionName = "2.1.${6 + runNumber}($runNumber)"

                val renamed = File(
                    outputDir,
                    "AFIExplorer-v$versionName($versionCode)-release.aab"
                )

                original.renameTo(renamed)
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Core / AndroidX
    implementation(libs.kotlin.stdlib)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.preference.ktx)
    implementation(libs.browser)
    implementation(libs.swiperefreshlayout)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.google.firebase.firestore.ktx)
    implementation(libs.work.runtime.ktx)
    ksp(libs.room.compiler)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // Firebase
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // UI / Misc
    implementation(libs.toasty)
    implementation(libs.oss.licenses)

    // Sheets
    implementation(libs.sheets.core)
    implementation(libs.sheets.info)
    implementation(libs.sheets.option)
    implementation(libs.sheets.input)
    implementation(libs.sheets.lottie)

    // Animations
    implementation(libs.android.lottie)
}