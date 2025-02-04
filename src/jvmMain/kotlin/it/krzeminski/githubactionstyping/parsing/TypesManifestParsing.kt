package it.krzeminski.githubactionstyping.parsing

import it.krzeminski.snakeyaml.engine.kmp.api.Load
import it.krzeminski.snakeyaml.engine.kmp.api.LoadSettings
import it.krzeminski.snakeyaml.engine.kmp.schema.CoreSchema

data class TypesManifest(
    val inputs: Map<String, ApiItem>? = null,
    val outputs: Map<String, ApiItem>? = null,
)

data class ApiItem(
    val type: String? = null,
    val name: String? = null,
    val allowedValues: List<String>? = null,
    val separator: String? = null,
    val namedValues: Map<String, Int>? = null,
    val listItem: ApiItem? = null,
)

fun parseTypesManifest(manifestString: String): Result<TypesManifest> =
    runCatching {
        val loadedTypesManifest = Load(
            // work-around for https://github.com/krzema12/snakeyaml-engine-kmp/pull/390
            LoadSettings.builder().setSchema(CoreSchema()).build()
        ).loadOne(manifestString)

        when (loadedTypesManifest) {
            null -> TypesManifest()

            is Map<*, *> -> {
                val excessKeys = loadedTypesManifest.keys - listOf("inputs", "outputs")
                if (excessKeys.isNotEmpty()) {
                    throw ValidationException(excessKeys.joinToString(prefix = "Excess keys: ", postfix = "."))
                }
                TypesManifest(
                    inputs = loadedTypesManifest.toInputsOrOutputs("inputs"),
                    outputs = loadedTypesManifest.toInputsOrOutputs("outputs"),
                )
            }

            else -> throw ValidationException("Types file must be a mapping.")
        }
    }

private fun Map<*, *>.toInputsOrOutputs(key: String): Map<String, ApiItem>? =
    when (val inputsOrOutputs = this[key]) {
        null -> null

        is Map<*, *> -> {
            inputsOrOutputs.entries.associate { (key, value) -> key as String to value.toApiItem(key) }
        }

        else -> throw ValidationException("$key must be a mapping.")
    }

private fun Any?.toApiItem(key: String): ApiItem =
    when (this) {
        null -> ApiItem()

        is Map<*, *> -> {
            val excessKeys = keys - listOf(
                "type",
                "name",
                "allowed-values",
                "separator",
                "named-values",
                "list-item",
            )
            if (excessKeys.isNotEmpty()) {
                throw ValidationException(excessKeys.joinToString(prefix = "Excess keys: ", postfix = "."))
            }

            ApiItem(
                type = get("type")?.let {
                    if (it !is String) {
                        throw ValidationException("type must be a string.")
                    }
                    it
                },
                name = get("name")?.let {
                    if (it !is String) {
                        throw ValidationException("name must be a string.")
                    }
                    it
                },
                allowedValues = get("allowed-values")?.let {
                    if (it !is List<*>) {
                        throw ValidationException("allowed-values must be a sequence.")
                    }
                    it.map {
                        if (it !is String) {
                            throw ValidationException("Allowed Value must be string.")
                        }
                        it
                    }
                },
                separator = get("separator")?.let {
                    if (it !is String) {
                        throw ValidationException("separator must be string.")
                    }
                    it
                },
                namedValues = get("named-values")?.let {
                    if (it !is Map<*, *>) {
                        throw ValidationException("named-values must be a mapping.")
                    }
                    it.mapKeys { (key, _) -> key as String }.mapValues { (_, value) ->
                        if (value !is Int) {
                            throw ValidationException("Named values must be integer.")
                        }
                        value
                    }
                },
                listItem = toApiItem("list-item"),
            )
        }

        else -> throw ValidationException("$key must be a mapping.")
    }

private fun Map<*, *>.toApiItem(key: String): ApiItem? = get(key)?.toApiItem(key)

internal class ValidationException(override val message: String) : Exception(message)
