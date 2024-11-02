package it.krzeminski.githubactionstyping.validation

import it.krzeminski.githubactionstyping.parsing.ApiItem
import it.krzeminski.githubactionstyping.parsing.TypesManifest
import it.krzeminski.githubactionstyping.validation.types.validateBoolean
import it.krzeminski.githubactionstyping.validation.types.validateEnum
import it.krzeminski.githubactionstyping.validation.types.validateFloat
import it.krzeminski.githubactionstyping.validation.types.validateInteger
import it.krzeminski.githubactionstyping.validation.types.validateList
import it.krzeminski.githubactionstyping.validation.types.validateString
import java.nio.file.Path

fun TypesManifest.validate(manifestPath: Path): RepoValidationResult {
    val inputValidationResults = this.inputs.mapValues { (_, value) -> value.validate() }
    val outputValidationResults = this.outputs.mapValues { (_, value) -> value.validate() }
    val isSomethingInvalid = (inputValidationResults.values + outputValidationResults.values)
        .any { it != ItemValidationResult.Valid }

    return RepoValidationResult(
        overallResult = if (isSomethingInvalid) ItemValidationResult.Invalid("Some typing is invalid.") else ItemValidationResult.Valid,
        pathToActionValidationResult = mapOf(manifestPath to ActionValidationResult(
            overallResult = if (isSomethingInvalid) ItemValidationResult.Invalid("Some typing is invalid.") else ItemValidationResult.Valid,
            inputs = inputValidationResults,
            outputs = outputValidationResults,
        )
        )
    )
}

fun buildInputOutputMismatchValidationResult(
    manifestPath: Path,
    inputsInManifest: Set<String>,
    inputsInTypesManifest: Set<String>,
    outputsInManifest: Set<String>,
    outputsInTypesManifest: Set<String>,
): RepoValidationResult {
    return RepoValidationResult(
        overallResult = ItemValidationResult.Invalid("There was input/output mismatch for one of the actions."),
        pathToActionValidationResult = mapOf(manifestPath to ActionValidationResult(
            overallResult = ItemValidationResult.Invalid(
                "Input/output mismatch detected. Please fix it first, then rerun to see other possible violations.",
            ),
            inputs = (inputsInManifest + inputsInTypesManifest)
                .associateWith {
                    if (it in inputsInManifest && it in inputsInTypesManifest) {
                        ItemValidationResult.Valid
                    } else {
                        if (it !in inputsInManifest) {
                            ItemValidationResult.Invalid("This input doesn't exist in the action manifest.")
                        } else {
                            ItemValidationResult.Invalid("This input doesn't exist in the types manifest.")
                        }
                    }
                },
            outputs = (outputsInManifest + outputsInTypesManifest)
                .associateWith {
                    if (it in outputsInManifest && it in outputsInTypesManifest) {
                        ItemValidationResult.Valid
                    } else {
                        if (it !in outputsInManifest) {
                            ItemValidationResult.Invalid("This output doesn't exist in the action manifest.")
                        } else {
                            ItemValidationResult.Invalid("This output doesn't exist in the types manifest.")
                        }
                    }
                },
            )
        )
    )
}

private fun ApiItem.validate(): ItemValidationResult {
    if (this.type == null) {
        return ItemValidationResult.Invalid("Type must be specified. Use 'type' attribute.")
    }

    return when (this.type) {
        "string" -> this.validateString()
        "boolean" -> this.validateBoolean()
        "integer" -> this.validateInteger()
        "float" -> this.validateFloat()
        "list" -> this.validateList()
        "enum" -> this.validateEnum()
        else -> ItemValidationResult.Invalid("Unknown type: '${this.type}'.")
    }
}
