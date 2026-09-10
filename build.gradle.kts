plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room.plugin) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        android.set(true)
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("detekt.yml"))
        parallel = true
        // Débito hoje existente (comentário fora de // TODO, pacote não batendo
        // com diretório, etc.) fica suprimido aqui — qualquer violação NOVA
        // continua bloqueando o build. Gerado uma vez com detektBaseline*; para
        // reduzir o débito, remova a entrada do XML depois de corrigir o código.
        baseline = file("detekt-baseline.xml")
    }

    tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektWarn") {
        description = "Orçamento de complexidade/tamanho e heurísticas — nunca falha o build."
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("detekt-warn.yml"))
        ignoreFailures = true
        setSource(files(projectDir))
        include("**/*.kt")
    }

    tasks.withType<org.gradle.api.tasks.SourceTask>().configureEach {
        if (name.startsWith("detekt")) {
            exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**", "**/generated/**", "**/composeResources/**")
            // detekt 1.23.8 lança NullPointerException interna ao analisar uma
            // extension function com receiver totalmente qualificado inline
            // (`private fun android.location.Location.isFresh()`) — bug do motor,
            // não do código. Excluído até atualizar detekt ou reescrever a
            // assinatura. Precisa valer tanto para Detekt quanto para
            // DetektCreateBaselineTask — os dois são SourceTask.
            exclude("**/LocationProvider.android.kt")
        }
    }
}

subprojects {
    dependencies {
        add("detektPlugins", rootProject.libs.compose.rules.detekt)
    }
}

// A task `detekt` padrão não agrega nada em módulo Kotlin Multiplatform — o plugin
// cria uma task por target/source-set (detektMetadataCommonMain, detektAndroidDebug,
// detektIosArm64Main, ...) e nenhuma delas depende das outras. `detektAll` amarra as
// duas que cobrem código de verdade (commonMain, compartilhado pelos 3 alvos, e
// androidMain) num único comando de "lint". Registrado dentro de
// `projectsEvaluated` porque os subprojetos só ganham essas tasks depois que o
// script deles roda — buscar antes disso via findByName sempre voltaria nulo.
gradle.projectsEvaluated {
    tasks.register("detektAll") {
        description = "Roda detekt (tier error) em commonMain + androidMain de todo módulo."
        subprojects.forEach { subproject ->
            subproject.tasks.findByName("detektMetadataCommonMain")?.let { dependsOn(it) }
            subproject.tasks.findByName("detektAndroidDebug")?.let { dependsOn(it) }
        }
    }
    tasks.register("detektBaselineAll") {
        description = "(Re)gera o detekt-baseline.xml de cada módulo com o débito atual."
        subprojects.forEach { subproject ->
            subproject.tasks.findByName("detektBaselineMetadataCommonMain")?.let { dependsOn(it) }
            subproject.tasks.findByName("detektBaselineAndroidDebug")?.let { dependsOn(it) }
        }
    }
}
