package it.krzeminski.githubactionstyping.validation.types

import it.krzeminski.githubactionstyping.parsing.ApiItem
import it.krzeminski.githubactionstyping.validation.ItemValidationResult

fun ApiItem.validateInteger(): ItemValidationResult {
    if ((this.namedValues == null) && (this.name != null)) {
        return ItemValidationResult.Invalid("'name' is only allowed for this type when also having 'named-values'.")
    }
    if (this.namedValues?.isEmpty() == true) {
        return ItemValidationResult.Invalid("There must be at least one named value.")
    }
    if (this.namedValues?.keys?.any { it.isBlank() } == true) {
        return ItemValidationResult.Invalid("Named value names must not be empty.")
    }
    if (this.name != null && this.name.isBlank()) {
        return ItemValidationResult.Invalid("Name must not be empty.")
    }
    if (this.allowedValues != null) {
        return ItemValidationResult.Invalid("'allowed-values' is not allowed for this type.")
    }
    if (this.separator != null) {
        return ItemValidationResult.Invalid("'separator' is not allowed for this type.")
    }
    if (this.listItem != null) {
        return ItemValidationResult.Invalid("'list-item' is not allowed for this type.")
    }

    return ItemValidationResult.Valid
}
