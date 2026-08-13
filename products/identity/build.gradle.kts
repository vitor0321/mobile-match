import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    id("org.jetbrains.kotlin.native.cocoapods")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
        summary = "Match identity feature"
        homepage = "https://github.com/walcker/mobile-match"
        ios.deploymentTarget = "18.2"
        podfile = project.file("../../iosApp/Podfile")
        pod("FirebaseAuth")
        pod("FirebaseFunctions")
        pod("GoogleSignIn")
        pod("PurchasesHybridCommon") {
            version = "17.55.1"
        }
        pod("RevenueCat") {
            version = "5.67.1"
        }

        framework {
            baseName = "Identity"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.navigator)
            implementation(projects.cedarDS)

            implementation(libs.lyricist.core)
            implementation(libs.datastore.preferences)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.koin)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.purchasesKmpCore)
            implementation(libs.purchasesKmpDatetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.playServicesAuth)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.auth)
            implementation(libs.firebase.functions)
            implementation(libs.google.identity.googleid)
            implementation(libs.kotlinx.coroutines.play.services)
        }
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlin.testJunit)
        }
    }
}

android {
    namespace = "com.walcker.identity"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
