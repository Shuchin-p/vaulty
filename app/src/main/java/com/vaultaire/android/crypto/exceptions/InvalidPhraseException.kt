package com.vaultaire.android.crypto.exceptions

/**
 * Exception thrown when recovery phrase authentication fails.
 * Used for both wrong phrase and phrase verification failures.
 */
class InvalidPhraseException(message: String = "Invalid recovery phrase") : SecurityException(message) {

    constructor(cause: Throwable) : this("Invalid recovery phrase") {
        initCause(cause)
    }

    constructor(message: String, cause: Throwable) : this(message) {
        initCause(cause)
    }
}