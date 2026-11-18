package com.vaultaire.android.sharing

/**
 * The plaintext vault payload that is encrypted into an [EncryptedSharePackage].
 *
 * The payload carries the vault display name, a pseudonymous owner id, the
 * list of shared files, the Master Vault Key wrapped with the share key, and
 * the encrypted vault index. The recipient decrypts this payload, then uses
 * the unwrapped MVK with the existing VaultStorageEngine to read files.
 *
 * @property version Payload schema version. Current value: 1.
 * @property vaultName Human-readable vault name (may contain unicode).
 * @property ownerId Pseudonymous identifier, never PII.
 * @property files Files included in this share version.
 * @property masterKeyWrapped MVK encrypted with the share key (AES-GCM, 32-byte key + 16-byte tag).
 * @property fileIndex Encrypted vault index blob (reuse of the existing vault index format).
 */
@kotlinx.serialization.Serializable
data class SharePayload(
    val version: Int = 1,
    val vaultName: String,
    val ownerId: String,
    val files: List<SharedFile>,
    val masterKeyWrapped: ByteArray,
    val fileIndex: ByteArray,
) {
    init {
        require(version > 0) { "version must be positive" }
        require(vaultName.length <= 4096) { "vaultName too long" }
        require(ownerId.isNotEmpty()) { "ownerId must not be empty" }
        require(files.size <= 1000) { "files size exceeds 1000 spec limit" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SharePayload

        if (version != other.version) return false
        if (vaultName != other.vaultName) return false
        if (ownerId != other.ownerId) return false
        if (files != other.files) return false
        if (!masterKeyWrapped.contentEquals(other.masterKeyWrapped)) return false
        if (!fileIndex.contentEquals(other.fileIndex)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + vaultName.hashCode()
        result = 31 * result + ownerId.hashCode()
        result = 31 * result + files.hashCode()
        result = 31 * result + masterKeyWrapped.contentHashCode()
        result = 31 * result + fileIndex.contentHashCode()
        return result
    }
}
