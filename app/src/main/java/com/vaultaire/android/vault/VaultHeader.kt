package com.vaultaire.android.vault

import com.vaultaire.android.crypto.EncryptedEnvelope
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Vault Envelope Header stored at the beginning of vault metadata files.
 * Designed so disk observers cannot determine vault owners, vault names, or file counts.
 */
data class VaultHeader(
    val magic: ByteArray = "VLTHDR1".toByteArray(Charsets.UTF_8), // 7 Bytes
    val vaultId: String, // 36-char UUID string
    val isDuressDecoy: Boolean = false,
    val envelope: EncryptedEnvelope
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VaultHeader

        if (!magic.contentEquals(other.magic)) return false
        if (vaultId != other.vaultId) return false
        if (isDuressDecoy != other.isDuressDecoy) return false
        if (envelope != other.envelope) return false

        return true
    }

    override fun hashCode(): Int {
        var result = magic.contentHashCode()
        result = 31 * result + vaultId.hashCode()
        result = 31 * result + isDuressDecoy.hashCode()
        result = 31 * result + envelope.hashCode()
        return result
    }

    fun serialize(outputStream: OutputStream) {
        val idBytes = vaultId.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(7 + 4 + idBytes.size + 1 + 4 + envelope.iv.size + 4 + envelope.ciphertext.size)
        buffer.put(magic)
        buffer.putInt(idBytes.size)
        buffer.put(idBytes)
        buffer.put(if (isDuressDecoy) 0x01.toByte() else 0x00.toByte())
        buffer.putInt(envelope.iv.size)
        buffer.put(envelope.iv)
        buffer.putInt(envelope.ciphertext.size)
        buffer.put(envelope.ciphertext)
        outputStream.write(buffer.array())
    }

    companion object {
        fun deserialize(inputStream: InputStream): VaultHeader {
            val magicBytes = ByteArray(7)
            inputStream.read(magicBytes)
            require(magicBytes.contentEquals("VLTHDR1".toByteArray(Charsets.UTF_8))) { "Invalid Vault Header Magic" }

            val intBuf = ByteArray(4)
            inputStream.read(intBuf)
            val idLen = ByteBuffer.wrap(intBuf).int
            val idBytes = ByteArray(idLen)
            inputStream.read(idBytes)
            val vaultId = String(idBytes, Charsets.UTF_8)

            val duressByte = inputStream.read()
            val isDuressDecoy = duressByte == 0x01

            inputStream.read(intBuf)
            val ivLen = ByteBuffer.wrap(intBuf).int
            val iv = ByteArray(ivLen)
            inputStream.read(iv)

            inputStream.read(intBuf)
            val cipherLen = ByteBuffer.wrap(intBuf).int
            val ciphertext = ByteArray(cipherLen)
            inputStream.read(ciphertext)

            return VaultHeader(
                magic = magicBytes,
                vaultId = vaultId,
                isDuressDecoy = isDuressDecoy,
                envelope = EncryptedEnvelope(iv, ciphertext)
            )
        }
    }
}
