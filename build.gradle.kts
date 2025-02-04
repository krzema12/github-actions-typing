import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    kotlin("multiplatform") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("io.kotest.multiplatform") version "6.0.0.M1"
    distribution
}

val collectCatalogTypings by tasks.registering(Sync::class) {
    from("../github-actions-typing-catalog/typings")
    include("**/action-types.yml")
    into(layout.buildDirectory.dir("catalog-typings"))
    eachFile {
        relativePath = RelativePath(
            relativePath.isFile,
            relativePath
                .segments
                .joinToString("_")
                .replace("_action-types", "")
        )
    }
    includeEmptyDirs = false
}

val collectExternalTypings by tasks.registering(Sync::class) {
    mapOf(
        "Vampire_setup-wsl_v4.yml" to "Vampire/setup-wsl/refs/heads/v4/action-types.yml",
        "Vampire_setup-wsl_master.yml" to "Vampire/setup-wsl/refs/heads/master/action-types.yml",
        "typesafegithub_github-actions-typing_v1.yml" to "typesafegithub/github-actions-typing/refs/heads/v1/action-types.yml",
        "typesafegithub_github-actions-typing_main.yml" to "typesafegithub/github-actions-typing/refs/heads/main/action-types.yml",
    ).forEach { (name, location) ->
        from(resources.text.fromUri("https://raw.githubusercontent.com/$location")) {
            rename { name }
        }
    }
    into(layout.buildDirectory.dir("external-typings"))
}

val schemaFile = file("github-actions-typing.schema.json")
val goodDir = file("src/test/resources/good-typings")
val badDir = file("src/test/resources/bad-typings")

kotlin {
    jvmToolchain(21)
    jvm {
        mainRun {
            mainClass = "it.krzeminski.githubactionstyping.MainKt"
        }

        val test by testRuns.existing {
            executionTask {
                useJUnitPlatform()
                configureInputsForSchemaTest()
                systemProperty("schemaFile", schemaFile.absolutePath)
                systemProperty("catalogDir", collectCatalogTypings.get().destinationDir.absolutePath)
                systemProperty("externalDir", collectExternalTypings.get().destinationDir.absolutePath)
                systemProperty("goodDir", goodDir.absolutePath)
                systemProperty("badDir", badDir.absolutePath)
            }
        }
    }

    js {
        useEsModules()
        nodejs {
            testTask {
                configureInputsForSchemaTest()
                environment("schemaFile", schemaFile.absolutePath)
                environment("catalogDir", collectCatalogTypings.get().destinationDir.absolutePath)
                environment("externalDir", collectExternalTypings.get().destinationDir.absolutePath)
                environment("goodDir", goodDir.absolutePath)
                environment("badDir", badDir.absolutePath)
            }
        }
    }

    sourceSets {
        jvmMain {
            dependencies {
                implementation("com.charleskorn.kaml:kaml:0.70.0")
                implementation("it.krzeminski:snakeyaml-engine-kmp:3.1.0")
            }
        }

        jvmTest {
            dependencies {
                runtimeOnly("org.junit.platform:junit-platform-launcher")
                implementation(dependencies.platform("io.kotest:kotest-bom:5.9.1"))
                runtimeOnly("io.kotest:kotest-runner-junit5")
                implementation("io.kotest:kotest-framework-api")
                implementation("io.kotest:kotest-framework-datatest")
                implementation("io.kotest:kotest-assertions-core")

                implementation("it.krzeminski:snakeyaml-engine-kmp:3.1.0")
                implementation("io.github.optimumcode:json-schema-validator:0.3.1")
            }
        }

        jsTest {
            dependencies {
                implementation(dependencies.platform("io.kotest:kotest-bom:5.9.1"))
                implementation("io.kotest:kotest-framework-engine")
                implementation("io.kotest:kotest-framework-api")
                implementation("io.kotest:kotest-framework-datatest")
                implementation("io.kotest:kotest-assertions-core")

                implementation(kotlinWrappers.js)
                implementation(kotlinWrappers.node)
                implementation(kotlinWrappers.ajv)
                implementation(kotlinWrappers.yaml)
                implementation(kotlinWrappers.prantlf.jsonlint)
            }
        }
    }
}

kotlinNodeJsRootExtension.version = "20.18.1"

// disable the KMP plugin adding custom repositories which is bad practice
// and promotes supply chain attacks
// instead we define the repositories ourselves in the settings script
kotlinNodeJsRootExtension.downloadBaseUrl = null
yarn.downloadBaseUrl = null

distributions {
    main {
        contents {
            into("lib") {
                val jvmJar by tasks.existing
                from(jvmJar)
                val jvmRuntimeClasspath by configurations
                from(jvmRuntimeClasspath)
            }
        }
    }
}

fun AbstractTestTask.configureInputsForSchemaTest() {
    inputs
        .file(schemaFile)
        .withPropertyName("schemaFile")
        .normalizeLineEndings()
        .withPathSensitivity(NAME_ONLY)

    inputs
        .files(collectCatalogTypings)
        .withPropertyName("catalogDir")
        .normalizeLineEndings()
        .withPathSensitivity(NAME_ONLY)

    inputs
        .files(collectExternalTypings)
        .withPropertyName("externalDir")
        .normalizeLineEndings()
        .withPathSensitivity(NAME_ONLY)

    inputs
        .dir(goodDir)
        .withPropertyName("goodDir")
        .normalizeLineEndings()
        .withPathSensitivity(NAME_ONLY)

    inputs
        .dir(badDir)
        .withPropertyName("badDir")
        .normalizeLineEndings()
        .withPathSensitivity(NAME_ONLY)
}
