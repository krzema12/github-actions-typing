package it.krzeminski.githubactionstyping

import it.krzeminski.githubactionstyping.parsing.ValidationException
import it.krzeminski.githubactionstyping.parsing.parseManifest
import it.krzeminski.githubactionstyping.parsing.parseTypesManifest
import it.krzeminski.githubactionstyping.reporting.appendStatus
import it.krzeminski.githubactionstyping.reporting.toPlaintextReport
import it.krzeminski.githubactionstyping.validation.ItemValidationResult
import it.krzeminski.githubactionstyping.validation.buildInputOutputMismatchValidationResult
import it.krzeminski.githubactionstyping.validation.validate
import java.nio.file.Path

fun manifestsToReport(manifestAndPath: Pair<String, Path>?, typesManifest: String?): Pair<Boolean, String> {
    if (manifestAndPath == null) {
        return Pair(false, "No action manifest (action.yml or action.yaml) found!")
    }

    if (typesManifest == null) {
        return Pair(false, buildString {
            appendLine("For action with manifest at '${manifestAndPath.second}':")
            appendLine("Result:")
            val validationResult = ItemValidationResult.Invalid(
                "No types manifest (action-types.yml or action-types.yaml) found!")
            validationResult.appendStatus(this)
        })
    }

    val parsedTypesManifest =
        parseTypesManifest(typesManifest)
            .onFailure {
                if (it is ValidationException) {
                    return false to it.message
                }
                throw it
            }.getOrThrow()
    val parsedManifest = parseManifest(manifestAndPath.first)

    val inputsInTypesManifest = parsedTypesManifest.inputs?.keys ?: emptySet()
    val inputsInManifest = parsedManifest.inputs.keys

    val outputsInTypesManifest = parsedTypesManifest.outputs?.keys ?: emptySet()
    val outputsInManifest = parsedManifest.outputs.keys

    if (inputsInManifest != inputsInTypesManifest || outputsInManifest != outputsInTypesManifest) {
        val inputOutputMismatchValidationResult = buildInputOutputMismatchValidationResult(
            manifestPath = manifestAndPath.second,
            inputsInManifest = inputsInManifest,
            inputsInTypesManifest = inputsInTypesManifest,
            outputsInManifest = outputsInManifest,
            outputsInTypesManifest = outputsInTypesManifest,
        )
        return Pair(false, inputOutputMismatchValidationResult.toPlaintextReport())
    }

    printlnDebug("Action's manifest:")
    printlnDebug(manifestAndPath.first)

    printlnDebug("Parsed manifest:")
    printlnDebug(parsedManifest)

    printlnDebug("Action's types manifest:")
    printlnDebug(typesManifest)

    printlnDebug("Parsed types manifest:")
    printlnDebug(parsedTypesManifest)

    printlnDebug()
    printlnDebug("==============================================")
    printlnDebug()

    val validationResult = parsedTypesManifest.validate(manifestAndPath.second)
    val isValid = validationResult.overallResult is ItemValidationResult.Valid
    val report = validationResult.toPlaintextReport()

    return Pair(isValid, report)
}

private fun printlnDebug(string: Any? = null) =
    (string ?: "").toString().lines()
        .map { "::debug::$it" }
        .forEach { println(it) }
