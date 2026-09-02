import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.androidLibrary)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.walcker.games.screenshottests"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions { jvmTarget = JvmTarget.JVM_21 }
    }
}

val gamesProject = project(":products:games")

tasks.withType<KotlinCompile>().configureEach {
    val isDebug = name == "compileDebugUnitTestKotlin"
    val isRelease = name == "compileReleaseUnitTestKotlin"
    if (isDebug || isRelease) {
        val variant = if (isDebug) "Debug" else "Release"
        val variantLower = variant.lowercase()

        dependsOn(
            ":products:games:compile${variant}KotlinAndroid",
            ":products:games:bundleLibCompileToJar$variant",
        )

        val friendArtifacts =
            gamesProject.files(
                gamesProject.layout.buildDirectory.dir("tmp/kotlin-classes/$variantLower"),
                gamesProject.layout.buildDirectory.file("intermediates/compile_library_classes_jar/$variantLower/bundleLibCompileToJar$variant/classes.jar"),
                gamesProject.layout.buildDirectory.file("intermediates/aar_main_jar/$variantLower/syncLib${variant}Jars/classes.jar"),
            )

        @Suppress("UNCHECKED_CAST")
        val friendPaths =
            javaClass.methods
                .firstOrNull { it.name == "getFriendPaths" }
                ?.invoke(this) as? ConfigurableFileCollection

        friendPaths?.from(friendArtifacts)
    }
}

dependencies {
    implementation(projects.cedarDS)
    implementation(projects.core)
    implementation(projects.products.games)

    testImplementation(libs.compose.runtime)
    testImplementation(libs.compose.foundation)
    testImplementation(libs.compose.material3)
    testImplementation(libs.compose.ui)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.kotlinx.collections.immutable)
    testImplementation(libs.lyricist.core)
}
