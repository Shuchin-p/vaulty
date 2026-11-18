package com.vaultaire.android.sharing

import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

/**
 * Custom serializer for [SharePayload] and [SharedFile] using kotlinx.serialization JSON.
 *
 * Supports:
 * - Up to 1000 files per payload
 * - Unicode filenames and vault names
 * - Binary blobs (masterKeyWrapped, fileIndex, thumbnails) via base64
 * - Throws [IllegalArgumentException] with descriptive message on corrupt/malformed data
 */
object SharePayloadSerializer {
    private val json = Json {
        ignoreUnknownKeys = false
        allowSpecialFloatingPointValues = false
        encodeDefaults = true
    }

    /**
     * Serializes a [SharePayload] to a UTF-8 byte array.
     */
    fun serialize(payload: SharePayload): ByteArray {
        return try {
            json.encodeToString(SharePayload.serializer(), payload).toByteArray(StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to serialize SharePayload: ${e.message}", e)
        }
    }

    /**
     * Deserializes a [SharePayload] from a UTF-8 byte array.
     *
     * @throws IllegalArgumentException if the input is not valid UTF-8, not valid JSON,
     *         does not match the schema, or contains more than 1000 files.
     */
    fun deserialize(data: ByteArray): SharePayload {
        return try {
            val str = String(data, StandardCharsets.UTF_8)
            json.decodeFromString(SharePayload.serializer(), str)
        } catch (e: kotlinx.serialization.SerializationException) {
            throw IllegalArgumentException("Corrupt SharePayload: ${e.message}", e)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid SharePayload data: ${e.message}", e)
        }
    }

    /**
     * Serializes a [SharedFile] to a UTF-8 byte array.
     */
    fun serializeFile(file: SharedFile): ByteArray {
        return try {
            json.encodeToString(SharedFile.serializer(), file).toByteArray(StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to serialize SharedFile: ${e.message}", e)
        }
    }

    /**
     * Deserializes a [SharedFile] from a UTF-8 byte array.
     */
    fun deserializeFile(data: ByteArray): SharedFile {
        return try {
            val str = String(data, StandardCharsets.UTF_8)
            json.decodeFromString(SharedFile.serializer(), str)
        } catch (e: kotlinx.serialization.SerializationException) {
            throw IllegalArgumentException("Corrupt SharedFile: ${e.message}", e)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid SharedFile data: ${e.message}", e)
        }
    }
}
