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

        val baseCode = 42
        val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0

        versionCode = baseCode + runNumber

        // If GitHub is building it, it becomes "2.1.6.55". On your local Android Studio, it stays "2.1.6.0"
        versionName = if (runNumber > 0) "2.1.6.$runNumber" else "2.1.6.0"

        vectorDrawables.useSupportLibrary = true
    }

    namespace = "com.drewcodesit.afiexplorer"

    applicationVariants.all {
        resValue("string", "versionName", versionName)

        val appVersion = versionName
        val appCode = versionCode
        val buildTypeName = buildType.name

        outputs.all {
            val output =
                this as com.android.build.gradle.internal.api.BaseVariantOutputImpl

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

                val outputDir = file("${layout.buildDirectory}/outputs/bundle/release")

                val original = outputDir.listFiles()?.firstOrNull { it.name.endsWith(".aab") }

                if (original != null) {
                    val renamed = File(
                        outputDir,
                        "AFIExplorer-v$versionName($versionCode)-release.aab"
                    )

                    original.renameTo(renamed)
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
    implementation(libs.google.firebase.firestore.ktx)
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