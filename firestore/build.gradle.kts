import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    id("org.jetbrains.kotlin.native.cocoapods")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        version = "1.0.0"
        summary = "Match firestore client"
        homepage = "https://github.com/walcker/mobile-match"
        ios.deploymentTarget = "18.2"
        podfile = project.file("../iosApp/Podfile")
        pod("FirebaseFirestoreInternal")
        pod("FirebaseFunctions")

        framework {
            baseName = "Firestore"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation("com.google.firebase:firebase-firestore-ktx")
            implementation("com.google.firebase:firebase-functions-ktx")
            implementation(libs.kotlinx.coroutines.play.services)
        }
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlin.testJunit)
        }
        iosMain.dependencies {
        }
    }
}

android {
    namespace = "com.walcker.match.firestore"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
