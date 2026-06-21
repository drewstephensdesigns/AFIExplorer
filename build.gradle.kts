/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

plugins {
    // These plugins are not applied here but can be applied in subprojects
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

tasks.register<Delete>("clean") {
    group = "build"
    description = "Deletes the root project build directory"
    delete(rootProject.layout.buildDirectory)
}