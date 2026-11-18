package com.vaultaire.android.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Android Keystore-backed encrypted storage for:
 * - Device salt (for pattern derivation)
 * - Recovery phrase (encrypted, retrievable for settings)
 * - Recovery envelope
 * - Pattern envelope
 * - Cloud recovery manifest
 */
open class SecureStorage(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "vaultaire_secrets"
        private const val KEY_DEVICE_SALT = "device_salt"
        private const val KEY_ENCRYPTED_PHRASE = "encrypted_phrase"
        private const val KEY_PATTERN_ENVELOPE = "pattern_envelope"
        private const val KEY_RECOVERY_ENVELOPE = "recovery_envelope"
        private const val KEY_CLOUD_MANIFEST = "cloud_manifest"

        private val json = Json { ignoreUnknownKeys = true }

        private fun encodeBase64(bytes: ByteArray): String {
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        private fun decodeBase64(str: String): ByteArray {
            return Base64.decode(str, Base64.NO_WRAP)
        }
    }

    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Device salt (for pattern KDF)
    open fun saveDeviceSalt(salt: ByteArray) {
        prefs.edit().putString(KEY_DEVICE_SALT, encodeBase64(salt)).apply()
    }

    open fun getDeviceSalt(): ByteArray? {
        val base64 = prefs.getString(KEY_DEVICE_SALT, null) ?: return null
        return try {
            decodeBase64(base64)
        } catch (e: Exception) {
            null
        }
    }

    open fun hasDeviceSalt(): Boolean {
        return prefs.contains(KEY_DEVICE_SALT)
    }

    // Recovery phrase (encrypted, for settings display)
    open fun saveEncryptedPhrase(phraseWords: List<String>) {
        val jsonStr = json.encodeToString(serializer<List<String>>(), phraseWords)
        prefs.edit().putString(KEY_ENCRYPTED_PHRASE, jsonStr).apply()
    }

    open fun getEncryptedPhrase(): List<String>? {
        val jsonStr = prefs.getString(KEY_ENCRYPTED_PHRASE, null) ?: return null
        return try {
            json.decodeFromString(serializer<List<String>>(), jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    // Pattern envelope
    open fun savePatternEnvelope(envelope: EncryptedEnvelope) {
        val jsonStr = json.encodeToString(EncryptedEnvelope.serializer(), envelope)
        prefs.edit().putString(KEY_PATTERN_ENVELOPE, jsonStr).apply()
    }

    open fun getPatternEnvelope(): EncryptedEnvelope? {
        val jsonStr = prefs.getString(KEY_PATTERN_ENVELOPE, null) ?: return null
        return try {
            json.decodeFromString(EncryptedEnvelope.serializer(), jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    // Recovery envelope
    open fun saveRecoveryEnvelope(envelope: EncryptedEnvelope) {
        val jsonStr = json.encodeToString(EncryptedEnvelope.serializer(), envelope)
        prefs.edit().putString(KEY_RECOVERY_ENVELOPE, jsonStr).apply()
    }

    open fun getRecoveryEnvelope(): EncryptedEnvelope? {
        val jsonStr = prefs.getString(KEY_RECOVERY_ENVELOPE, null) ?: return null
        return try {
            json.decodeFromString(EncryptedEnvelope.serializer(), jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    // Cloud recovery manifest
    open fun saveCloudManifest(manifest: CloudRecoveryManifest) {
        val jsonStr = json.encodeToString(CloudRecoveryManifest.serializer(), manifest)
        prefs.edit().putString(KEY_CLOUD_MANIFEST, jsonStr).apply()
    }

    open fun getCloudManifest(): CloudRecoveryManifest? {
        val jsonStr = prefs.getString(KEY_CLOUD_MANIFEST, null) ?: return null
        return try {
            json.decodeFromString(CloudRecoveryManifest.serializer(), jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    // Clear all (vault deletion)
    open fun clearAll() {
        prefs.edit().clear().apply()
    }
}
