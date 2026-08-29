import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

val releaseKeyAlias = localProps.getProperty("release.keyAlias")
val releaseKeyPassword = localProps.getProperty("release.keyPassword")
val releaseStoreFile = localProps.getProperty("release.storeFile")
val releaseStorePassword = localProps.getProperty("release.storePassword")
val admobAppId = localProps.getProperty("ADMOB_APP_ID", "ca-app-pub-8514371864627144~1123615258")
val admobBannerUnitId = localProps.getProperty("ADMOB_BANNER_UNIT_ID", "ca-app-pub-8514371864627144/6345620957")
val revenueCatAndroidKey = localProps.getProperty("REVENUECAT_ANDROID_KEY", "")

val googleMapsApiKey: String = localProps.getProperty("GOOGLE_MAPS_API_KEY")
    ?: providers.environmentVariable("GOOGLE_MAPS_API_KEY").orNull
    ?: ""

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "App"
            isStatic = true
            export(projects.core)
            export(projects.navigator)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.koin.android)
            implementation(libs.firebase.messaging)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compottie)
            implementation(libs.compottie.dot)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.voyager.navigator)

            api(projects.core)
            api(projects.navigator)
            implementation(projects.cedarDS)
            implementation(projects.firestore)
            implementation(projects.products.identity)
            implementation(projects.products.games)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlin.testJunit)
        }
    }
}

compose.resources {
    packageOfResClass = "com.walcker.match.app.generated.resources"
    generateResClass = always
}

android {
    namespace = "com.walcker.match.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        if (
            releaseKeyAlias != null &&
            releaseKeyPassword != null &&
            releaseStoreFile != null &&
            releaseStorePassword != null
        ) {
            create("release") {
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.walcker.match.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = libs.versions.android.version.code.get().toInt()
        versionName = libs.versions.android.version.name.get()
        manifestPlaceholders["admobAppId"] = admobAppId
        manifestPlaceholders["admobBannerUnitId"] = admobBannerUnitId
        manifestPlaceholders["googleMapsApiKey"] = googleMapsApiKey

        buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
        buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$admobBannerUnitId\"")
        buildConfigField("String", "REVENUECAT_ANDROID_KEY", "\"$revenueCatAndroidKey\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
            val testAdmobBannerUnitId = "ca-app-pub-3940256099942544/6300978111"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
            resValue("string", "app_name", "Join Play Dev")
            buildConfigField("Boolean", "IS_DEBUG", "true")
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            manifestPlaceholders["admobBannerUnitId"] = testAdmobBannerUnitId
            manifestPlaceholders["googleMapsApiKey"] = googleMapsApiKey
            buildConfigField("String", "ADMOB_APP_ID", "\"$testAdmobAppId\"")
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$testAdmobBannerUnitId\"")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "Join Play")
            buildConfigField("Boolean", "IS_DEBUG", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

if (googleMapsApiKey.isBlank()) {
    logger.warn(
        "GOOGLE_MAPS_API_KEY ausente — o mapa não vai carregar neste build. " +
            "Defina em local.properties ou exporte a variável de ambiente.",
    )
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    doFirst {
        check(googleMapsApiKey.isNotBlank()) {
            "GOOGLE_MAPS_API_KEY ausente. Defina em local.properties ou na variável de ambiente " +
                "antes de gerar um build de release."
        }
    }
}
