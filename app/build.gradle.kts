plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.drewstephenscoding.afiexplorer"
        minSdk = 24
        targetSdk = 36

        // changed 20 MAY 26
        // version code = 41
        // version name = 2.1.5
        versionCode = 41
        versionName = "2.1.5"

        vectorDrawables.useSupportLibrary = true
    }

    namespace = "com.drewcodesit.afiexplorer"

    applicationVariants.all {
        resValue("string", "versionName", versionName)

        val appVersion = versionName
        val appCode = versionCode

        outputs.all {
            val output =
                this as com.android.build.gradle.internal.api.BaseVariantOutputImpl

            val buildTypeName = buildType.name

            output.outputFileName =
                "AFIExplorer-v$appVersion($appCode)-$buildTypeName.apk"
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            resValue("bool", "FIREBASE_ANALYTICS_DEACTIVATED", "true")
            ext["enableCrashlytics"] = false
        }

        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            resValue("bool", "FIREBASE_ANALYTICS_DEACTIVATED", "false")
            ext["enableCrashlytics"] = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
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

    tasks.whenTaskAdded {

        if (name == "bundleRelease") {

            doLast {

                val versionName = android.defaultConfig.versionName
                val versionCode = android.defaultConfig.versionCode

                val originalAab =
                    file("${layout.buildDirectory}/outputs/bundle/release/app-release.aab")

                val renamedAab =
                    file(
                        "${layout.buildDirectory}/outputs/bundle/release/" +
                                "AFIExplorer-v$versionName($versionCode)-release.aab"
                    )

                if (originalAab.exists()) {
                    originalAab.renameTo(renamedAab)
                }
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