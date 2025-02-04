package it.krzeminski.githubactionstyping

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beOfType
import it.krzeminski.githubactionstyping.parsing.ValidationException
import it.krzeminski.githubactionstyping.parsing.parseTypesManifest
import it.krzeminski.githubactionstyping.reporting.toPlaintextReport
import it.krzeminski.githubactionstyping.validation.ItemValidationResult
import it.krzeminski.githubactionstyping.validation.validate
import java.io.File

/**
 * Validate that the action validation logic agrees with the Json Schema.
 */
class LogicConsistencyTest : UseCaseTest() {
    override suspend fun testValid(typing: File) {
        val validationResult = parseTypesManifest(typing.readText()).getOrThrow().validate(typing.toPath().fileName)
        withClue(validationResult.toPlaintextReport()) {
            validationResult.overallResult shouldBe ItemValidationResult.Valid
        }
    }

    override suspend fun testInvalid(typing: File) {
        val typesManifest = typing.readText()
        val expectedValidationError = typesManifest
            .split("\n# Expected validation error\n")[1]
            .trimMargin("#")
            .trim()

        val parsedTypesManifest = parseTypesManifest(typesManifest)
        if (parsedTypesManifest.isFailure && parsedTypesManifest.exceptionOrNull() is ValidationException) {
            parsedTypesManifest.exceptionOrNull()!!.message shouldBe expectedValidationError
        } else {
            val validationResult = parsedTypesManifest.getOrThrow().validate(typing.toPath().fileName)
            assertSoftly {
                validationResult.overallResult should beOfType<ItemValidationResult.Invalid>()
                validationResult
                    .toPlaintextReport()
                    .trim()
                    .replace("\u001B", "\\x1B")
                    .shouldBe(expectedValidationError)
            }
        }
    }
}
