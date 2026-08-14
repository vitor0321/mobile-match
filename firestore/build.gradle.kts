import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    iosArm64()
    iosSimulatorArm64()

    // iOS cocoapods integration would be configured here once cocoapods plugin is available:
    // cocoapods {
    //     framework {
    //         baseName = "FirestoreKit"
    //         isStatic = false
    //         export(libs.koin.core)
    //     }
    //     pod("FirebaseFirestore") {
    //         version = "11.5.0"
    //     }
    // }

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
        iosMain.dependencies {
            // iOS requires: pod 'FirebaseFirestore' in iosApp/Podfile
            // Interop via Objective-C bridge in actual blocks
        }
    }
}

android {
    namespace = "com.walcker.match.firestore"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
