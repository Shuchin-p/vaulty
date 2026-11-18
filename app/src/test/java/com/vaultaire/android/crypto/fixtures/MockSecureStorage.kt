package com.vaultaire.android.crypto.fixtures

import com.vaultaire.android.crypto.EncryptedEnvelope

/**
 * Mock SecureStorage for unit tests.
 * Stores envelopes and phrase in memory instead of EncryptedSharedPreferences.
 * Does NOT extend SecureStorage (which requires Context and Keystore).
 */
class MockSecureStorage {
    private var patternEnvelope: EncryptedEnvelope? = null
    private var recoveryEnvelope: EncryptedEnvelope? = null
    private var encryptedPhrase: List<String>? = null
    private var deviceSalt: ByteArray? = null

    fun savePatternEnvelope(envelope: EncryptedEnvelope) {
        patternEnvelope = envelope
    }

    fun getPatternEnvelope(): EncryptedEnvelope? = patternEnvelope

    fun saveRecoveryEnvelope(envelope: EncryptedEnvelope) {
        recoveryEnvelope = envelope
    }

    fun getRecoveryEnvelope(): EncryptedEnvelope? = recoveryEnvelope

    fun saveEncryptedPhrase(phraseWords: List<String>) {
        encryptedPhrase = phraseWords
    }

    fun getEncryptedPhrase(): List<String>? = encryptedPhrase

    fun saveDeviceSalt(salt: ByteArray) {
        deviceSalt = salt
    }

    fun getDeviceSalt(): ByteArray? = deviceSalt

    fun hasDeviceSalt(): Boolean = deviceSalt != null

    fun clearAll() {
        patternEnvelope = null
        recoveryEnvelope = null
        encryptedPhrase = null
        deviceSalt = null
    }
}
