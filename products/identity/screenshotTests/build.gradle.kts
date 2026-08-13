import io.netty.buffer.ByteBufUtil.isAccessible
import org.gradle.internal.component.external.descriptor.MavenScope
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.lang.reflect.Proxy

fun Any.findDeclaredMethodRecursively(methodName: String) = generateSequence(javaClass) { it.superclass }
    .flatMap { it.declaredMethods.asSequence() }
    .firstOrNull { it.name == methodName }

plugins {
    alias(libs.plugins.androidLibrary)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.walcker.identity.screenshottests"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
}

val identityProject = project(":products:identity")

tasks.withType<KotlinCompile>().configureEach {
    val isDebug = name == "compileDebugUnitTestKotlin"
    val isRelease = name == "compileReleaseUnitTestKotlin"
    if (isDebug || isRelease) {
        val variant = if (isDebug) "Debug" else "Release"
        val variantLower = variant.lowercase()

        dependsOn(
            ":products:identity:compile${variant}KotlinAndroid",
            ":products:identity:bundleLibCompileToJar${variant}",
        )

        val friendArtifacts = identityProject.files(
            identityProject.layout.buildDirectory.dir("tmp/kotlin-classes/$variantLower"),
            identityProject.layout.buildDirectory.file("intermediates/compile_library_classes_jar/$variantLower/bundleLibCompileToJar${variant}/classes.jar"),
            identityProject.layout.buildDirectory.file("intermediates/aar_main_jar/$variantLower/syncLib${variant}Jars/classes.jar"),
        )

        @Suppress("UNCHECKED_CAST")
        val friendPaths = javaClass.methods
            .firstOrNull { it.name == "getFriendPaths" }
            ?.invoke(this) as? ConfigurableFileCollection

        friendPaths?.from(friendArtifacts)
    }
}

afterEvaluate {
    tasks.withType<Test>().configureEach {
        if (name == "testDebugUnitTest" || name == "testReleaseUnitTest") {
            val reporterInterface = Class.forName("org.gradle.api.internal.tasks.testing.report.TestReporter")
            val noOpReporter = Proxy.newProxyInstance(
                reporterInterface.classLoader,
                arrayOf(reporterInterface),
            ) { proxy, method, args ->
                when (method.name) {
                    "generateReport" -> null
                    "toString" -> "NoOpTestReporter"
                    "hashCode" -> MavenScope.System.identityHashCode(proxy)
                    "equals" -> proxy === args?.firstOrNull()
                    else -> null
                }
            }

            findDeclaredMethodRecursively("setTestReporter")
                ?.apply { isAccessible = true }
                ?.invoke(this, noOpReporter)
        }
    }
}

dependencies {
    implementation(projects.cedarDS)
    implementation(projects.core)
    implementation(projects.products.identity)

    testImplementation(libs.compose.runtime)
    testImplementation(libs.compose.foundation)
    testImplementation(libs.compose.material3)
    testImplementation(libs.compose.ui)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.kotlinx.collections.immutable)
    testImplementation(libs.lyricist.core)
}
