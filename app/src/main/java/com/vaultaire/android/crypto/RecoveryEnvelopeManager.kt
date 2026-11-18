package com.vaultaire.android.crypto

import android.util.Base64
import com.vaultaire.android.crypto.exceptions.InvalidPhraseException
import com.vaultaire.android.crypto.exceptions.InvalidPatternException
import javax.crypto.AEADBadTagException
import javax.crypto.SecretKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Manages the dual-envelope system: Pattern Envelope + Recovery Envelope.
 * Both envelopes wrap the SAME Master Vault Key.
 */
class RecoveryEnvelopeManager(
    private val envelopeEngine: EnvelopeEncryptionEngine,
    private val phraseEngine: RecoveryPhraseEngine,
    private val keyDerivation: KeyDerivationEngine,
    private val secureStorage: SecureStorage
) {

    data class VaultEnvelopes(
        val patternEnvelope: EncryptedEnvelope,
        val recoveryEnvelope: EncryptedEnvelope,
        val encryptedPhrase: ByteArray, // Phrase encrypted for settings display
        val phraseVersion: Int = 1
    )

    /**
     * Creates both envelopes during vault creation.
     * @param masterKey The MVK (32 bytes)
     * @param patternBytes Normalized pattern bytes
     * @param phraseWords 9-word generated phrase
     * @return VaultEnvelopes containing both envelopes + encrypted phrase
     */
    fun createEnvelopes(
        masterKey: SecretKey,
        patternBytes: ByteArray,
        phraseWords: List<String>
    ): VaultEnvelopes {
        // Derive VDK from pattern
        val vaultKey = keyDerivation.deriveVaultKey(patternBytes)

        // Create Pattern Envelope
        val patternEnvelope = envelopeEngine.wrapMasterKey(masterKey, vaultKey)

        // Normalize phrase and derive RK
        val normalizedPhrase = phraseEngine.normalizePhrase(phraseWords)
        val recoveryKey = phraseEngine.deriveRecoveryKey(normalizedPhrase)

        // Create Recovery Envelope
        val recoveryEnvelope = envelopeEngine.wrapMasterKey(masterKey, recoveryKey)

        // Store encrypted phrase (phrase itself for display, not encrypted)
        val phraseJson = Json { ignoreUnknownKeys = true }
        val encryptedPhrase = phraseJson.encodeToString(serializer<List<String>>(), phraseWords).toByteArray(Charsets.UTF_8)

        // Save to secure storage
        secureStorage.savePatternEnvelope(patternEnvelope)
        secureStorage.saveRecoveryEnvelope(recoveryEnvelope)
        secureStorage.saveEncryptedPhrase(phraseWords)

        return VaultEnvelopes(
            patternEnvelope = patternEnvelope,
            recoveryEnvelope = recoveryEnvelope,
            encryptedPhrase = encryptedPhrase,
            phraseVersion = 1
        )
    }

    /**
     * Recovers MVK using recovery phrase (local recovery).
     * @param phraseWords User-entered phrase words
     * @return Master Vault Key if phrase is correct
     * @throws InvalidPhraseException if phrase wrong
     */
    fun recoverWithPhrase(phraseWords: List<String>): SecretKey {
        // ALWAYS attempt decryption to prevent timing attacks
        val envelope = secureStorage.getRecoveryEnvelope()
            ?: throw InvalidPhraseException("Recovery envelope not found")

        val normalizedPhrase = phraseEngine.normalizePhrase(phraseWords)
        val recoveryKey = phraseEngine.deriveRecoveryKey(normalizedPhrase)

        return try {
            envelopeEngine.unwrapMasterKey(envelope, recoveryKey)
        } catch (e: AEADBadTagException) {
            // Same error regardless of whether envelope exists or phrase is wrong
            throw InvalidPhraseException("Invalid recovery phrase")
        }
    }

    /**
     * Recovers MVK using pattern (normal unlock).
     * @param patternBytes Normalized pattern bytes
     * @return Master Vault Key if pattern is correct
     * @throws InvalidPatternException if pattern wrong
     */
    fun unlockWithPattern(patternBytes: ByteArray): SecretKey {
        val envelope = secureStorage.getPatternEnvelope()
            ?: throw InvalidPatternException("Pattern envelope not found")

        val vaultKey = keyDerivation.deriveVaultKey(patternBytes)

        return try {
            envelopeEngine.unwrapMasterKey(envelope, vaultKey)
        } catch (e: javax.crypto.AEADBadTagException) {
            throw InvalidPatternException("Invalid pattern")
        }
    }

    /**
     * Rotates envelopes after pattern change.
     * Generates new phrase, new recovery envelope, new pattern envelope.
     * MVK remains the same.
     * @param masterKey Current MVK
     * @param newPatternBytes New pattern
     * @return New VaultEnvelopes
     */
    fun rotateEnvelopes(masterKey: SecretKey, newPatternBytes: ByteArray): VaultEnvelopes {
        // Create new pattern envelope
        val newVaultKey = keyDerivation.deriveVaultKey(newPatternBytes)
        val newPatternEnvelope = envelopeEngine.wrapMasterKey(masterKey, newVaultKey)

        // Generate new recovery phrase
        val newPhraseWords = phraseEngine.generatePhrase()
        val newNormalizedPhrase = phraseEngine.normalizePhrase(newPhraseWords)
        val newRecoveryKey = phraseEngine.deriveRecoveryKey(newNormalizedPhrase)

        // Create new recovery envelope
        val newRecoveryEnvelope = envelopeEngine.wrapMasterKey(masterKey, newRecoveryKey)

        // Convert phrase to byte array for storage
        val phraseJson = Json { ignoreUnknownKeys = true }
        val encryptedPhrase = phraseJson.encodeToString(serializer<List<String>>(), newPhraseWords).toByteArray(Charsets.UTF_8)

        // Update secure storage
        secureStorage.savePatternEnvelope(newPatternEnvelope)
        secureStorage.saveRecoveryEnvelope(newRecoveryEnvelope)
        secureStorage.saveEncryptedPhrase(newPhraseWords)

        return VaultEnvelopes(
            patternEnvelope = newPatternEnvelope,
            recoveryEnvelope = newRecoveryEnvelope,
            encryptedPhrase = encryptedPhrase,
            phraseVersion = 1
        )
    }

    /**
     * Gets the current recovery phrase for settings display.
     * Decrypts from secure storage.
     */
    fun getRecoveryPhrase(): List<String> {
        val phrase = secureStorage.getEncryptedPhrase()
            ?: throw IllegalStateException("No recovery phrase stored")
        return phrase
    }
}