package com.vaultaire.android.crypto.exceptions

/**
 * Exception thrown when pattern authentication fails.
 * Used for both wrong pattern and pattern verification failures.
 */
class InvalidPatternException(message: String = "Invalid pattern") : SecurityException(message) {

    constructor(cause: Throwable) : this("Invalid pattern") {
        initCause(cause)
    }

    constructor(message: String, cause: Throwable) : this(message) {
        initCause(cause)
    }
}