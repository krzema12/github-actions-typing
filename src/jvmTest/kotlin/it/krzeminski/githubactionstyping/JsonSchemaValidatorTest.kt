package it.krzeminski.githubactionstyping

import io.github.optimumcode.json.schema.JsonSchema
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.file.shouldBeADirectory
import io.kotest.matchers.file.shouldNotBeEmptyDirectory
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.kotest.mpp.sysprop
import it.krzeminski.snakeyaml.engine.kmp.api.Load
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

private val schemaFile = File(sysprop("schemaFile")!!)
private val catalogDir = File(sysprop("catalogDir")!!)
private val externalDir = File(sysprop("externalDir")!!)
private val goodDir = File(sysprop("goodDir")!!)
private val badDir = File(sysprop("badDir")!!)

private lateinit var schema: JsonSchema

class JsonSchemaValidatorTest : FunSpec({
    beforeSpec {
        catalogDir.shouldBeADirectory()
        catalogDir.shouldNotBeEmptyDirectory()

        schema = schemaFile
            .inputStream()
            .use {
                JsonSchema.fromDefinition(it.reader().readText())
            }
    }

    context("catalog typings") {
        withData(
            nameFn = { it.name },
            catalogDir.listFiles()!!.asSequence()
        ) {
            it.shouldBeValid()
        }
    }

    if (externalDir.isDirectory) {
        context("external typings") {
            withData(
                nameFn = { it.name },
                externalDir.listFiles()!!.asSequence()
            ) {
                it.shouldBeValid()
            }
        }
    }

    context("valid typings") {
        withData(
            nameFn = { it.name },
            goodDir.listFiles()!!.asSequence()
        ) {
            it.shouldBeValid()
        }
    }

    context("invalid typings") {
        withData(
            nameFn = { it.name },
            badDir.listFiles()!!.asSequence()
        ) {
            it.shouldNotBeValid()
        }
    }
})

private fun beValid(): Matcher<File> {
    return Matcher { dataFile ->
        var errorMessage: String? = null
        MatcherResult(
            runCatching {
                schema.validate(dataFile.inputStream().use { Load().loadOne(it) }.toJsonElement()) {
                    errorMessage = buildString {
                        if (errorMessage != null) {
                            appendLine(errorMessage)
                        }
                        append("\tinstancePath: ")
                        appendLine(it.objectPath)
                        append("\tschemaPath: ")
                        appendLine(it.schemaPath)
                        append("\tmessage: ")
                        appendLine(it.message)
                    }
                }
            }.getOrElse {
                errorMessage = it.message
                false
            },
            { "schema validation of ${dataFile.name} failed:\n$errorMessage" },
            { "${dataFile.name} should have failed schema validation but passed" },
        )
    }
}

private fun File.shouldBeValid(): File {
    this should beValid()
    return this
}

private fun File.shouldNotBeValid(): File {
    this shouldNot beValid()
    return this
}

// work-around for https://github.com/OptimumCode/json-schema-validator/issues/194 (direct support for Kotlin classes)
// or https://github.com/OptimumCode/json-schema-validator/issues/195 (direct support for snakeyaml Node)
// or https://github.com/OptimumCode/json-schema-validator/issues/190 (direct support for kaml YamlNode)
private fun Any?.toJsonElement(): JsonElement {
    return when (this) {
        is Map<*, *> -> JsonObject(entries.associate { (key, value) -> "$key" to value.toJsonElement() })
        is List<*> -> JsonArray(map { it.toJsonElement() })
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        null -> JsonNull
        else -> error("Unexpected type: ${this::class.qualifiedName}")
    }
}