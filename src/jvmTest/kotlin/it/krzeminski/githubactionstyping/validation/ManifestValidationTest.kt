package it.krzeminski.githubactionstyping.validation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import it.krzeminski.githubactionstyping.parsing.ApiItem
import it.krzeminski.githubactionstyping.parsing.TypesManifest
import kotlin.io.path.Path

class ManifestValidationTest : FunSpec({
    context("success cases") {
        test("primitive types") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "string-input" to ApiItem(type = "string"),
                    "boolean-input" to ApiItem(type = "boolean"),
                    "integer-input" to ApiItem(type = "integer"),
                    "integer-with-named-values-input" to ApiItem(
                        type = "integer",
                        namedValues = mapOf("foo" to 1, "bar" to 2)
                    ),
                    "integer-with-named-values-and-custom-item-name-input" to ApiItem(
                        type = "integer",
                        name = "SomeItemName",
                        namedValues = mapOf("foo" to 1, "bar" to 2)
                    ),
                    "float-input" to ApiItem(type = "float"),
                ),
            )

            // when
            val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Valid,
                inputs = mapOf(
                    "string-input" to ItemValidationResult.Valid,
                    "boolean-input" to ItemValidationResult.Valid,
                    "integer-input" to ItemValidationResult.Valid,
                    "integer-with-named-values-input" to ItemValidationResult.Valid,
                    "integer-with-named-values-and-custom-item-name-input" to ItemValidationResult.Valid,
                    "float-input" to ItemValidationResult.Valid,
                ),
            )
        }

        test("enum type") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "enum-input" to ApiItem(type = "enum", allowedValues = listOf("foo", "bar", "baz")),
                    "enum-input-with-custom-item-name" to ApiItem(
                        type = "enum",
                        name = "SomeItemName",
                        allowedValues = listOf("foo", "bar", "baz"),
                    ),
                ),
            )

            // when
            val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Valid,
                inputs = mapOf(
                    "enum-input" to ItemValidationResult.Valid,
                    "enum-input-with-custom-item-name" to ItemValidationResult.Valid,
                ),
            )
        }

        test("list type") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "list-of-strings-input" to ApiItem(
                        type = "list",
                        separator = "\n",
                        listItem = ApiItem(type = "string"),
                    ),
                    "list-of-booleans-input" to ApiItem(
                        type = "list",
                        separator = "\n",
                        listItem = ApiItem(type = "boolean"),
                    ),
                    "list-of-integers-input" to ApiItem(
                        type = "list",
                        separator = "\n",
                        listItem = ApiItem(type = "integer"),
                    ),
                    "list-of-floats-input" to ApiItem(
                        type = "list",
                        separator = "\n",
                        listItem = ApiItem(type = "float"),
                    ),
                    "list-of-enums-input" to ApiItem(
                        type = "list",
                        separator = "\n",
                        listItem = ApiItem(
                            type = "enum",
                            allowedValues = listOf("foo", "bar")
                        ),
                    ),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Valid,
                inputs = mapOf(
                    "list-of-strings-input" to ItemValidationResult.Valid,
                    "list-of-booleans-input" to ItemValidationResult.Valid,
                    "list-of-integers-input" to ItemValidationResult.Valid,
                    "list-of-floats-input" to ItemValidationResult.Valid,
                    "list-of-enums-input" to ItemValidationResult.Valid,
                ),
            )
        }
    }

    context("failure cases") {
        test("input and output without type") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "some-input" to ApiItem(type = null),
                ),
                outputs = mapOf(
                    "some-output" to ApiItem(type = null),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "some-input" to ItemValidationResult.Invalid(
                        message = "Type must be specified. Use 'type' attribute."
                    ),
                ),
                outputs = mapOf(
                    "some-output" to ItemValidationResult.Invalid(
                        message = "Type must be specified. Use 'type' attribute."
                    ),
                ),
            )
        }

        test("unknown type") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "some-input" to ApiItem(type = "for-sure-unknown-type"),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "some-input" to ItemValidationResult.Invalid(
                        message = "Unknown type: 'for-sure-unknown-type'."
                    ),
                ),
            )
        }

        test("primitive types with 'allowed-values' attribute") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "string-input" to ApiItem(type = "string", allowedValues = listOf("foo", "bar")),
                    "boolean-input" to ApiItem(type = "boolean", allowedValues = listOf("foo", "bar")),
                    "integer-input" to ApiItem(type = "integer", allowedValues = listOf("foo", "bar")),
                    "float-input" to ApiItem(type = "float", allowedValues = listOf("foo", "bar")),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "string-input" to ItemValidationResult.Invalid("'allowed-values' is not allowed for this type."),
                    "boolean-input" to ItemValidationResult.Invalid("'allowed-values' is not allowed for this type."),
                    "integer-input" to ItemValidationResult.Invalid("'allowed-values' is not allowed for this type."),
                    "float-input" to ItemValidationResult.Invalid("'allowed-values' is not allowed for this type."),
                ),
            )
        }

        test("primitive types with 'separator' attribute") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "string-input" to ApiItem(type = "string", separator = ","),
                    "boolean-input" to ApiItem(type = "boolean", separator = ","),
                    "integer-input" to ApiItem(type = "integer", separator = ","),
                    "float-input" to ApiItem(type = "float", separator = ","),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "string-input" to ItemValidationResult.Invalid("'separator' is not allowed for this type."),
                    "boolean-input" to ItemValidationResult.Invalid("'separator' is not allowed for this type."),
                    "integer-input" to ItemValidationResult.Invalid("'separator' is not allowed for this type."),
                    "float-input" to ItemValidationResult.Invalid("'separator' is not allowed for this type."),
                ),
            )
        }

        test("non-list types with 'list-item' attribute") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "string-input" to ApiItem(type = "string", listItem = ApiItem(type = "string")),
                    "boolean-input" to ApiItem(type = "boolean", listItem = ApiItem(type = "string")),
                    "integer-input" to ApiItem(type = "integer", listItem = ApiItem(type = "string")),
                    "float-input" to ApiItem(type = "float", listItem = ApiItem(type = "string")),
                    "enum-input" to ApiItem(
                        type = "enum",
                        allowedValues = listOf("foo", "bar"),
                        listItem = ApiItem(type = "string"),
                    ),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "string-input" to ItemValidationResult.Invalid("'list-item' is not allowed for this type."),
                    "boolean-input" to ItemValidationResult.Invalid("'list-item' is not allowed for this type."),
                    "integer-input" to ItemValidationResult.Invalid("'list-item' is not allowed for this type."),
                    "float-input" to ItemValidationResult.Invalid("'list-item' is not allowed for this type."),
                    "enum-input" to ItemValidationResult.Invalid("'list-item' is not allowed for this type."),
                ),
            )
        }

        test("enum type with 'separator' attribute") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "enum-input" to ApiItem(type = "enum", allowedValues = listOf("foo", "bar", "baz"), separator = ","),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "enum-input" to ItemValidationResult.Invalid("'separator' is not allowed for this type."),
                ),
            )
        }

        test("enum type without 'allowed-values' attribute") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "enum-input" to ApiItem(type = "enum", allowedValues = null),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "enum-input" to ItemValidationResult.Invalid("Allowed values must be specified."),
                ),
            )
        }

        test("enum type with just one allowed value") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "enum-input" to ApiItem(type = "enum", allowedValues = listOf("foo")),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Valid,
                inputs = mapOf(
                    "enum-input" to ItemValidationResult.Valid,
                ),
            )
        }

        test("enum type with empty list of allowed value") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "enum-input" to ApiItem(type = "enum", allowedValues = emptyList()),
                ),
            )

            // when
            val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "enum-input" to ItemValidationResult.Invalid("There must be at least one allowed value."),
                ),
            )
        }

        test("list type without 'list-item' attribute") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "list-input" to ApiItem(type = "list", separator = ","),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "list-input" to ItemValidationResult.Invalid("List item information must be specified."),
                ),
            )
        }

        test("list type without 'separator' attribute") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "list-input" to ApiItem(type = "list", listItem = ApiItem(type = "string")),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "list-input" to ItemValidationResult.Invalid("Separator must be specified."),
                ),
            )
        }

        test("list type with 'allowed-values' attribute") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "list-input" to ApiItem(
                        type = "list",
                        separator = "\n",
                        allowedValues = listOf("foo", "bar"),
                        listItem = ApiItem(type = "string"),
                    ),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "list-input" to ItemValidationResult.Invalid("'allowed-values' is not allowed for this type."),
                ),
            )
        }

        test("list type with forbidden list item type") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "list-of-lists-input" to ApiItem(
                        type = "list",
                        separator = "\n",
                        listItem = ApiItem(
                            type = "list",
                            separator = ",",
                            listItem = ApiItem(type = "string"),
                        ),
                    ),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "list-of-lists-input" to ItemValidationResult.Invalid(
                        "List item type: List can be parameterized only with a primitive or enum type.",
                    ),
                ),
            )
        }

        test("list type with list item type with incorrect attributes") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "list-of-enums-without-allowed-values-input" to ApiItem(
                        type = "list",
                        separator = "\n",
                        listItem = ApiItem(type = "enum"),
                    ),
                    "list-of-integers-with-allowed-values-input" to ApiItem(
                        type = "list",
                        separator = "\n",
                        listItem = ApiItem(type = "integer", allowedValues = listOf("foo", "bar")),
                    ),
                    "list-of-unknown-type-input" to ApiItem(
                        type = "list",
                        separator = "\n",
                        listItem = ApiItem(type = "for-sure-unknown-type"),
                    ),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "list-of-enums-without-allowed-values-input" to ItemValidationResult.Invalid(
                        "List item type: Allowed values must be specified.",
                    ),
                    "list-of-integers-with-allowed-values-input" to ItemValidationResult.Invalid(
                        "List item type: 'allowed-values' is not allowed for this type."
                    ),
                    "list-of-unknown-type-input" to ItemValidationResult.Invalid(
                        "List item type: Unknown type: 'for-sure-unknown-type'."
                    )
                ),
            )
        }

        test("non-integer types with named values") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "string-input" to ApiItem(type = "string", namedValues = mapOf("foo" to 1)),
                    "boolean-input" to ApiItem(type = "boolean", namedValues = mapOf("foo" to 1)),
                    "float-input" to ApiItem(type = "float", namedValues = mapOf("foo" to 1)),
                    "list-input" to ApiItem(type = "list", separator = ",", listItem = ApiItem(type = "string"), namedValues = mapOf("foo" to 1)),
                    "enum-input" to ApiItem(type = "enum", allowedValues = listOf("foo", "bar"), namedValues = mapOf("foo" to 1)),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "string-input" to ItemValidationResult.Invalid("'named-values' are currently supported only for integers."),
                    "boolean-input" to ItemValidationResult.Invalid("'named-values' are currently supported only for integers."),
                    "float-input" to ItemValidationResult.Invalid("'named-values' are currently supported only for integers."),
                    "list-input" to ItemValidationResult.Invalid("'named-values' are currently supported only for integers."),
                    "enum-input" to ItemValidationResult.Invalid("'named-values' are currently supported only for integers."),
                ),
            )
        }

        test("non-enum types with 'name' attribute") {
            // given
            val manifest = TypesManifest(
                inputs = mapOf(
                    "string-input" to ApiItem(type = "string", name = "SomeName"),
                    "boolean-input" to ApiItem(type = "boolean", name = "SomeName"),
                    "integer-input" to ApiItem(type = "integer", name = "SomeName"),
                    "float-input" to ApiItem(type = "float", name = "SomeName"),
                    "list-input" to ApiItem(type = "list", name = "SomeName", separator = ",", listItem = ApiItem(type = "string")),
                ),
            )

            // when
             val result = manifest.validate(Path("action.yml")).pathToActionValidationResult[Path("action.yml")]

            // then
            result shouldBe ActionValidationResult(
                overallResult = ItemValidationResult.Invalid("Some typing is invalid."),
                inputs = mapOf(
                    "string-input" to ItemValidationResult.Invalid("'name' is not allowed for this type."),
                    "boolean-input" to ItemValidationResult.Invalid("'name' is not allowed for this type."),
                    "integer-input" to ItemValidationResult.Invalid("'name' is only allowed for this type when also having 'named-values'."),
                    "float-input" to ItemValidationResult.Invalid("'name' is not allowed for this type."),
                    "list-input" to ItemValidationResult.Invalid("'name' is not allowed for this type."),
                ),
            )
        }
    }
})
