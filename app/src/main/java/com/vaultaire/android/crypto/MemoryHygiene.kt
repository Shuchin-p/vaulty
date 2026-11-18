package com.vaultaire.android.crypto

import java.util.Arrays
import javax.crypto.SecretKey

object MemoryHygiene {

    /**
     * Overwrites a byte array with zeroes in RAM.
     */
    fun wipe(bytes: ByteArray?) {
        if (bytes != null) {
            Arrays.fill(bytes, 0.toByte())
        }
    }

    /**
     * Wipes multiple byte arrays with zeroes in RAM.
     */
    fun wipeAll(vararg bytes: ByteArray?) {
        bytes.forEach { wipe(it) }
    }

    /**
     * Wipes a char array with zeroes in RAM.
     */
    fun wipeChars(chars: CharArray?) {
        if (chars != null) {
            Arrays.fill(chars, '\u0000')
        }
    }

    /**
     * Wipes secret key material from memory if accessible.
     */
    fun wipeKey(key: SecretKey?) {
        key?.encoded?.let { wipe(it) }
    }
}