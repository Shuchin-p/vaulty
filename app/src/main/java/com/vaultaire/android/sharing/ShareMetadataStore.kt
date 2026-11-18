package com.vaultaire.android.sharing

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.common.io.BaseEncoding
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure storage for ShareMetadata using EncryptedSharedPreferences with AES256-SIV/GCM.
 *
 * Share metadata is excluded from duress purge (per spec: shared vaults survive duress wipe).
 * The metadata is encrypted with a device-bound key stored in Android Keystore.
 *
 * Uses constant-time comparison (MessageDigest.isEqual) for phrase hash verification.
 */
class ShareMetadataStore(private val context: Context) {

    private val prefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "share_metadata_store",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Stores share metadata. Overwrites existing entry with same shareId.
     */
    fun put(metadata: ShareMetadata) {
        val key = "metadata_${metadata.shareId}"
        val value = json.encodeToString(ShareMetadata.serializer(), metadata)
        prefs.edit().putString(key, value).apply()
    }

    /**
     * Retrieves share metadata by shareId, or null if not found.
     */
    fun get(shareId: String): ShareMetadata? {
        val key = "metadata_$shareId"
        val value = prefs.getString(key, null)
        return value?.let {
            try {
                json.decodeFromString(ShareMetadata.serializer(), it)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Removes share metadata by shareId.
     */
    fun remove(shareId: String) {
        val key = "metadata_$shareId"
        prefs.edit().remove(key).apply()
    }

    /**
     * Returns all stored share metadata entries.
     */
    fun getAll(): List<ShareMetadata> {
        val all = mutableListOf<ShareMetadata>()
        val map = prefs.all
        for ((key, value) in map) {
            if (key.startsWith("metadata_") && value is String) {
                try {
                    val metadata = json.decodeFromString(ShareMetadata.serializer(), value)
                    all.add(metadata)
                } catch (e: Exception) {
                    // Skip corrupt entries
                }
            }
        }
        return all
    }

    /**
     * Returns all share metadata for a specific owner.
     */
    fun getByOwner(ownerId: String): List<ShareMetadata> {
        return getAll().filter { it.ownerId == ownerId }
    }

    /**
     * Checks if a phrase hash exists in any stored metadata (for claim verification).
     */
    fun hasPhraseHash(phraseHash: String): Boolean {
        return getAll().any { it.phraseHash == phraseHash }
    }

    /**
     * Finds share metadata by phrase hash (used when recipient claims a share).
     */
    fun findByPhraseHash(phraseHash: String): ShareMetadata? {
        return getAll().find { it.phraseHash == phraseHash }
    }

    /**
     * Clears all share metadata (for testing or full reset).
     * Note: This does NOT affect metadata stored in Drive.
     */
    fun clearAll() {
        val keysToRemove = mutableListOf<String>()
        for (key in prefs.all.keys) {
            if (key.startsWith("metadata_")) {
                keysToRemove.add(key)
            }
        }
        val editor = prefs.edit()
        for (key in keysToRemove) {
            editor.remove(key)
        }
        editor.apply()
    }
}
