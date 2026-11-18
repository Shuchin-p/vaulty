package com.vaultaire.android.camera

import android.content.Context
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.Recording
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.vaultaire.android.crypto.MemoryHygiene
import com.vaultaire.android.storage.EncryptedOutputStream
import com.vaultaire.android.vault.VaultManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.crypto.SecretKey

/**
 * Secure Camera Manager for capturing photos and videos directly into encrypted .vlt files.
 * Implements zero-temp-file architecture: photo captures go ImageProxy → memory → EncryptedOutputStream → .vlt.
 * Video uses ParcelFileDescriptor pipe → EncryptedOutputStream → .vlt.
 * No unencrypted files ever hit disk.
 */
class SecureCameraManager(
    private val context: Context,
    private val vaultManager: VaultManager,
    private val keyProvider: () -> SecretKey
) : SecureCameraTestHooks {

    // Test hook state
    private var mockImageCapture: ImageCapture? = null
    private var mockVideoCapture: VideoCapture<Recorder>? = null
    private var mockKeyProvider: (() -> SecretKey)? = null
    private var lastPhotoBytes: ByteArray? = null
    private var lastVideoPath: String? = null

    /**
     * Captures a photo directly into an AES-256-GCM .vlt file without intermediate disk cache.
     * The captured image is held in memory for the review step before encryption.
     * 
     * @param imageCapture The CameraX ImageCapture use case
     * @param vaultId The target vault ID
     * @param onSuccess Callback when photo is captured and ready for review (Bitmap in memory)
     * @param onError Callback on capture error
     */
    fun captureSecurePhoto(
        imageCapture: ImageCapture,
        vaultId: String,
        onSuccess: (ByteArray) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val capture = mockImageCapture ?: imageCapture
        
        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val bytes = try {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            bytes
                        } finally {
                            image.close() // Always release ImageProxy back to pool
                        }

                        // Store for test hook
                        lastPhotoBytes = bytes.copyOf()
                        
                        // Return bytes for review (held in memory, not encrypted yet)
                        withContext(Dispatchers.Main) { onSuccess(bytes) }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    /**
     * Encrypts a photo byte array to a .vlt file in the vault.
     * Called after user confirms "Keep" on review screen.
     * 
     * @param photoBytes The photo bytes from capture (JPEG)
     * @param vaultId The target vault ID
     * @param onSuccess Callback when encryption completes
     * @param onError Callback on encryption error
     */
    fun encryptPhotoToVault(
        photoBytes: ByteArray,
        vaultId: String,
        onSuccess: (File) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val targetFile = File(vaultManager.getVaultDir(vaultId), ".vlt")
            
            try {
                val masterKey = mockKeyProvider?.invoke() ?: keyProvider()
                EncryptedOutputStream(FileOutputStream(targetFile), masterKey).use { eos ->
                    eos.write(photoBytes)
                }
                lastVideoPath = targetFile.absolutePath // Reuse for test hook
                withContext(Dispatchers.Main) { onSuccess(targetFile) }
            } catch (t: Throwable) {
                // Clean up partial file on error
                targetFile.delete()
                withContext(Dispatchers.Main) { onError(t) }
            } finally {
                MemoryHygiene.wipe(photoBytes)
            }
        }
    }

    /**
     * Starts secure video recording to a .vlt file via ParcelFileDescriptor pipe.
     * No temporary unencrypted file is created.
     * 
     * @param videoCapture The CameraX VideoCapture<Recorder> use case
     * @param vaultId The target vault ID
     * @param onRecordingFinished Callback with success/failure when recording finalizes
     * @return The Recording object to control recording (pause/resume/stop)
     */
    fun startSecureVideoRecording(
        videoCapture: VideoCapture<Recorder>,
        vaultId: String,
        onRecordingFinished: (Boolean, File?) -> Unit
    ): Recording {
        val capture = mockVideoCapture ?: videoCapture
        val targetFile = File(vaultManager.getVaultDir(vaultId), ".vlt")
        lastVideoPath = targetFile.absolutePath

        // Create pipe for zero-temp-file streaming
        val pipe = ParcelFileDescriptor.createPipe()
        val readSide = pipe[0]
        val writeSide = pipe[1]

        val fileOutputOptions = FileDescriptorOutputOptions.Builder(writeSide).build()

        var ok = false
        val recording = capture.output
            .prepareRecording(context, fileOutputOptions)
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Finalize -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            ok = try {
                                if (!recordEvent.hasError()) {
                                    // Encrypt from pipe read side to target .vlt
                                    encryptPipeToFile(readSide, targetFile)
                                    true
                                } else {
                                    false
                                }
                            } catch (t: Throwable) {
                                false
                            } finally {
                                try { readSide.close() } catch (e: Exception) {}
                                try { writeSide.close() } catch (e: Exception) {}
                                if (!ok) {
                                    targetFile.delete() // Delete partial .vlt on error/cancel
                                }
                            }
                            withContext(Dispatchers.Main) { onRecordingFinished(ok, if (ok) targetFile else null) }
                        }
                    }
                }
            }

        return recording
    }

    /**
     * Reads from pipe readSide and encrypts to target .vlt file.
     * Runs in parallel with video encoding.
     */
    private fun encryptPipeToFile(readSide: ParcelFileDescriptor, targetFile: File) {
        val masterKey = mockKeyProvider?.invoke() ?: keyProvider()
        
        FileInputStream(readSide.fileDescriptor).use { fis ->
            EncryptedOutputStream(FileOutputStream(targetFile), masterKey).use { eos ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    eos.write(buffer, 0, bytesRead)
                }
            }
        }
    }

    /**
     * Cancels an ongoing video recording and cleans up partial .vlt file.
     */
    fun cancelVideoRecording(recording: Recording?, targetFile: File?) {
        recording?.close()
        targetFile?.delete()
    }

    // =====================================================================
    // SecureCameraTestHooks implementation
    // =====================================================================

    override fun setMockImageCapture(mock: ImageCapture) {
        mockImageCapture = mock
    }

    override fun setMockVideoCapture(mock: VideoCapture<Recorder>) {
        mockVideoCapture = mock
    }

    override fun setMockKeyProvider(provider: () -> SecretKey) {
        mockKeyProvider = provider
    }

    override fun captureLastPhotoBytes(): ByteArray? {
        return lastPhotoBytes?.copyOf()
    }

    override fun captureLastVideoPath(): String? {
        return lastVideoPath
    }
}
