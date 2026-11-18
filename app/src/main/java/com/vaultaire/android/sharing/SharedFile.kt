package com.vaultaire.android.sharing

/**
 * A single file descriptor included in a [SharePayload].
 *
 * The thumbnail, when present, is itself encrypted (small, base64-friendly).
 * All byte fields use raw [ByteArray] so the serializer can round-trip them
 * without coupling to a specific encoding.
 */
@kotlinx.serialization.Serializable
data class SharedFile(
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val createdMs: Long,
    val thumbnail: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SharedFile

        if (fileId != other.fileId) return false
        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (size != other.size) return false
        if (createdMs != other.createdMs) return false
        if (!thumbnail.contentEquals(other.thumbnail)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileId.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + createdMs.hashCode()
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        return result
    }
}
