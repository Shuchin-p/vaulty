package com.vaultaire.android.sharing

import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Status of a share from the owner's perspective.
 */
enum class ShareStatus {
    ACTIVE,         // Share is active and usable
    EXPIRED,        // Share has passed its expiration date
    REVOKED,        // Owner revoked the share
    CLAIMED,        // Recipient has claimed the share (phrase spent)
    PENDING_UPLOAD, // Share created locally, waiting for Drive upload
    REVOKING,       // Revocation queued, waiting for Drive sync
}

/**
 * State of a shared vault from the recipient's perspective.
 */
enum class SharedVaultState {
    ACTIVE,         // Vault is accessible
    EXPIRED,        // Share has expired
    REVOKED,        // Owner revoked access
    PENDING_SYNC,   // Waiting for first sync / update
    ERROR,          // Error state (corrupt data, etc.)
}

/**
 * Exceptions for share operations.
 */
class ShareClaimedException(message: String) : SecurityException(message)
class ShareExpiredException(message: String) : SecurityException(message)
class ShareMaxOpensExceededException(message: String) : SecurityException(message)
class ExportDisabledException(message: String) : SecurityException(message)

/**
 * Metadata for a share, stored both locally (encrypted) and in Drive (metadata.json).
 *
 * This is the source of truth for share state. The local copy is encrypted with
 * the device key (AES256-SIV/GCM via EncryptedSharedPreferences) and excluded
 * from duress purge. The Drive copy is plaintext JSON (no secrets).
 *
 * @property shareId UUID v4, unique per share
 * @property phraseHash SHA-256 of the normalized sharing phrase (hex), used for claim verification
 * @property ownerId Pseudonymous owner identifier
 * @property vaultName Display name of the vault at share creation
 * @property policy Access policy (expiration, max opens, export disabled)
 * @property version Current package version (increments on updates)
 * @property revoked True if owner has revoked this share
 * @property createdMs Creation timestamp (epoch ms)
 * @property updatedMs Last update timestamp (epoch ms)
 * @property spent True if phrase has been claimed by a recipient
 * @property driveFileId Google Drive file ID for the share folder
 * @property localOpenCount Number of times recipient has opened (recipient side only)
 */
@Serializable
data class ShareMetadata(
    val shareId: String,
    val phraseHash: String,
    val ownerId: String,
    val vaultName: String,
    @Contextual
    val policy: SharePolicy,
    val version: Int = 1,
    val revoked: Boolean = false,
    val createdMs: Long = System.currentTimeMillis(),
    val updatedMs: Long = System.currentTimeMillis(),
    val spent: Boolean = false,
    val driveFileId: String? = null,
    val localOpenCount: Int = 0,
) {
    /**
     * Creates a new ShareMetadata for a newly created share.
     */
    companion object {
        fun createNew(
            shareId: String,
            phrase: String,
            ownerId: String,
            vaultName: String,
            policy: SharePolicy,
            driveFileId: String? = null,
        ): ShareMetadata {
            val phraseHash = computePhraseHash(SharingPhraseGenerator.normalizePhrase(phrase))
            val now = System.currentTimeMillis()
            return ShareMetadata(
                shareId = shareId,
                phraseHash = phraseHash,
                ownerId = ownerId,
                vaultName = vaultName,
                policy = policy,
                version = 1,
                revoked = false,
                createdMs = now,
                updatedMs = now,
                spent = false,
                driveFileId = driveFileId,
                localOpenCount = 0,
            )
        }

        private fun computePhraseHash(phrase: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(phrase.toByteArray(StandardCharsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * Verifies if the provided phrase matches this share's phrase hash.
     * Uses constant-time comparison to prevent timing attacks.
     */
    fun verifyPhrase(phrase: String): Boolean {
        val normalized = SharingPhraseGenerator.normalizePhrase(phrase)
        val inputHash = computePhraseHash(normalized)
        return MessageDigest.isEqual(
            phraseHash.toByteArray(StandardCharsets.US_ASCII),
            inputHash.toByteArray(StandardCharsets.US_ASCII)
        )
    }

    private fun computePhraseHash(phrase: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(phrase.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Returns true if the share is expired based on policy.
     */
    fun isExpired(): Boolean {
        return policy.isExpired()
    }

    /**
     * Returns true if the share is revoked.
     */
    fun isRevoked(): Boolean {
        return revoked
    }

    /**
     * Returns true if the share has been claimed (phrase spent).
     */
    fun isSpent(): Boolean {
        return spent
    }

    /**
     * Returns true if the share is active and usable.
     */
    fun isActive(): Boolean {
        return !isExpired() && !isRevoked() && !isSpent()
    }

    /**
     * Returns a display status for UI.
     */
    fun displayStatus(): ShareStatus {
        return when {
            isRevoked() -> ShareStatus.REVOKED
            isExpired() -> ShareStatus.EXPIRED
            isSpent() -> ShareStatus.CLAIMED
            else -> ShareStatus.ACTIVE
        }
    }

    /**
     * Creates a copy with incremented version and updated timestamp (for updates).
     */
    fun nextVersion(): ShareMetadata {
        return copy(
            version = version + 1,
            updatedMs = System.currentTimeMillis(),
        )
    }

    /**
     * Creates a copy marked as revoked.
     */
    fun revoked(): ShareMetadata {
        return copy(
            revoked = true,
            updatedMs = System.currentTimeMillis(),
        )
    }

    /**
     * Creates a copy marked as spent (phrase claimed).
     */
    fun spent(): ShareMetadata {
        return copy(
            spent = true,
            updatedMs = System.currentTimeMillis(),
        )
    }

    /**
     * Creates a copy with incremented local open count (recipient side).
     */
    fun incrementOpenCount(): ShareMetadata {
        return copy(
            localOpenCount = localOpenCount + 1,
            updatedMs = System.currentTimeMillis(),
        )
    }
}
