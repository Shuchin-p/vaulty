package com.vaultaire.android.crypto

import kotlinx.serialization.Serializable
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class EncryptedEnvelope(
    val iv: ByteArray,
    val ciphertext: ByteArray
)

class EnvelopeEncryptionEngine {

    companion object {
        private const val CIPHER_ALGO = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH_BYTES = 12
    }

    /**
     * Generates a fresh random 256-bit Master Vault Key (MVK).
     */
    fun generateMasterVaultKey(): SecretKey {
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        try {
            return SecretKeySpec(keyBytes, "AES")
        } finally {
            MemoryHygiene.wipe(keyBytes)
        }
    }

    /**
     * Wraps (encrypts) the Master Vault Key using the Vault Derivation Key (VDK).
     */
    fun wrapMasterKey(masterKey: SecretKey, derivationKey: SecretKey): EncryptedEnvelope {
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.ENCRYPT_MODE, derivationKey, spec)
        val encodedKey = masterKey.encoded
        val encryptedKeyBytes = try {
            cipher.doFinal(encodedKey)
        } finally {
            MemoryHygiene.wipe(encodedKey)
        }
        return EncryptedEnvelope(iv = iv, ciphertext = encryptedKeyBytes)
    }

    /**
     * Wraps (encrypts) the Master Vault Key using the Vault Derivation Key (VDK)
     * with Additional Authenticated Data (AAD).
     */
    fun wrapMasterKey(masterKey: SecretKey, derivationKey: SecretKey, aad: ByteArray): EncryptedEnvelope {
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.ENCRYPT_MODE, derivationKey, spec)
        cipher.updateAAD(aad)
        val encodedKey = masterKey.encoded
        val encryptedKeyBytes = try {
            cipher.doFinal(encodedKey)
        } finally {
            MemoryHygiene.wipe(encodedKey)
        }
        return EncryptedEnvelope(iv = iv, ciphertext = encryptedKeyBytes)
    }

    /**
     * Unwraps (decrypts) the Master Vault Key using the Vault Derivation Key (VDK).
     * Throws exception if derivation key (pattern) is incorrect (GCM tag validation failure).
     */
    fun unwrapMasterKey(envelope: EncryptedEnvelope, derivationKey: SecretKey): SecretKey {
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, envelope.iv)

        cipher.init(Cipher.DECRYPT_MODE, derivationKey, spec)
        val masterKeyBytes = cipher.doFinal(envelope.ciphertext)
        try {
            return SecretKeySpec(masterKeyBytes, "AES")
        } finally {
            MemoryHygiene.wipe(masterKeyBytes)
        }
    }

    /**
     * Unwraps (decrypts) the Master Vault Key using the Vault Derivation Key (VDK)
     * with Additional Authenticated Data (AAD).
     */
    fun unwrapMasterKey(envelope: EncryptedEnvelope, derivationKey: SecretKey, aad: ByteArray): SecretKey {
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, envelope.iv)

        cipher.init(Cipher.DECRYPT_MODE, derivationKey, spec)
        cipher.updateAAD(aad)
        val masterKeyBytes = cipher.doFinal(envelope.ciphertext)
        try {
            return SecretKeySpec(masterKeyBytes, "AES")
        } finally {
            MemoryHygiene.wipe(masterKeyBytes)
        }
    }
}
