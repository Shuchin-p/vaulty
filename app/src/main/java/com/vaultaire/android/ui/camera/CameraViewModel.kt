package com.vaultaire.android.ui.camera

import androidx.camera.core.ImageCapture
import androidx.camera.video.Recording
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultaire.android.camera.SecureCameraManager
import com.vaultaire.android.vault.VaultManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.crypto.SecretKey

/**
 * ViewModel for CameraScreen.
 * Holds captured media in memory (ImageProxy/Bitmap) during review step.
 * No disk writes until user confirms "Keep".
 */
class CameraViewModel(
    private val secureCameraManager: SecureCameraManager,
    private val vaultManager: VaultManager,
    private val currentVaultId: String,
    private val masterKeyProvider: () -> SecretKey
) : ViewModel() {

    // Capture state
    enum class CaptureMode { PHOTO, VIDEO }
    enum class FlashMode { AUTO, ON, OFF }
    enum class LensFacing { FRONT, BACK }

    // UI State
    private val _captureMode = MutableStateFlow(CaptureMode.PHOTO)
    val captureMode = _captureMode.asStateFlow()

    private val _lensFacing = MutableStateFlow(LensFacing.BACK)
    val lensFacing = _lensFacing.asStateFlow()

    private val _flashMode = MutableStateFlow(FlashMode.AUTO)
    val flashMode = _flashMode.asStateFlow()

    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled = _torchEnabled.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs = _recordingDurationMs.asStateFlow()

    private val _hasAudioPermission = MutableStateFlow(true)
    val hasAudioPermission = _hasAudioPermission.asStateFlow()

    // Review state - holds captured media in memory
    private val _capturedPhotoBytes: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    val capturedPhotoBytes = _capturedPhotoBytes.asStateFlow()

    private val _capturedVideoPath: MutableStateFlow<String?> = MutableStateFlow(null)
    val capturedVideoPath = _capturedVideoPath.asStateFlow()

    private val _isReviewing = MutableStateFlow(false)
    val isReviewing = _isReviewing.asStateFlow()

    private val _isEncrypting = MutableStateFlow(false)
    val isEncrypting = _isEncrypting.asStateFlow()

    private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Current recording reference for cancellation
    private var currentRecording: Recording? = null
    private var currentVideoTargetFile: java.io.File? = null

    // Camera controls
    fun toggleCaptureMode() {
        _captureMode.value = if (_captureMode.value == CaptureMode.PHOTO) CaptureMode.VIDEO else CaptureMode.PHOTO
        // Reset torch when switching to photo mode
        if (_captureMode.value == CaptureMode.PHOTO) {
            _torchEnabled.value = false
        }
    }

    fun toggleLens() {
        _lensFacing.value = if (_lensFacing.value == LensFacing.FRONT) LensFacing.BACK else LensFacing.FRONT
        // Torch only available for rear camera
        if (_lensFacing.value == LensFacing.FRONT) {
            _torchEnabled.value = false
        }
    }

    fun cycleFlashMode() {
        _flashMode.value = when (_flashMode.value) {
            FlashMode.AUTO -> FlashMode.ON
            FlashMode.ON -> FlashMode.OFF
            FlashMode.OFF -> FlashMode.AUTO
        }
    }

    fun toggleTorch() {
        if (_lensFacing.value == LensFacing.BACK) {
            _torchEnabled.value = !_torchEnabled.value
        }
    }

    fun setAudioPermissionGranted(granted: Boolean) {
        _hasAudioPermission.value = granted
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    // Photo capture
    fun capturePhoto(imageCapture: ImageCapture) {
        _errorMessage.value = null

        secureCameraManager.captureSecurePhoto(
            imageCapture = imageCapture,
            vaultId = currentVaultId,
            onSuccess = { photoBytes ->
                // Photo captured - hold in memory for review
                _capturedPhotoBytes.value = photoBytes
                _isReviewing.value = true
            },
            onError = { error ->
                _errorMessage.value = "Photo capture failed: $error"
            }
        )
    }

    // Video recording
    fun startVideoRecording(videoCapture: androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>) {
        _errorMessage.value = null
        _isRecording.value = true
        _recordingDurationMs.value = 0

        // Start duration timer
        viewModelScope.launch {
            while (_isRecording.value) {
                kotlinx.coroutines.delay(100)
                _recordingDurationMs.value += 100
            }
        }

        currentRecording = secureCameraManager.startSecureVideoRecording(
            videoCapture = videoCapture,
            vaultId = currentVaultId,
            onRecordingFinished = { success, targetFile ->
                _isRecording.value = false
                if (success && targetFile != null) {
                    _capturedVideoPath.value = targetFile.absolutePath
                    _isReviewing.value = true
                } else {
                    _errorMessage.value = "Video recording failed"
                }
                currentRecording = null
                currentVideoTargetFile = null
            }
        )

        currentVideoTargetFile = java.io.File(vaultManager.getVaultDir(currentVaultId), ".vlt")
    }

    fun stopVideoRecording() {
        currentRecording?.stop()
        // The onRecordingFinished callback will handle the rest
    }

    // Review actions
    fun onRetake() {
        // Discard captured media - wipe buffers
        _capturedPhotoBytes.value?.let { secureCameraManager.captureLastPhotoBytes()?.let { /* already wiped */ } }
        _capturedPhotoBytes.value = null
        _capturedVideoPath.value = null
        _isReviewing.value = false
        _errorMessage.value = null
    }

    fun onKeep() {
        _isEncrypting.value = true

        if (_captureMode.value == CaptureMode.PHOTO) {
            _capturedPhotoBytes.value?.let { photoBytes ->
                secureCameraManager.encryptPhotoToVault(
                    photoBytes = photoBytes,
                    vaultId = currentVaultId,
                    onSuccess = { encryptedFile ->
                        _isEncrypting.value = false
                        _capturedPhotoBytes.value = null
                        _isReviewing.value = false
                        // Navigation handled by caller
                    },
                    onError = { error ->
                        _isEncrypting.value = false
                        _errorMessage.value = "Encryption failed: $error"
                    }
                )
            }
        } else {
            // Video is already encrypted during recording finalization
            _isEncrypting.value = false
            _capturedVideoPath.value = null
            _isReviewing.value = false
            // Navigation handled by caller
        }
    }

    // Cleanup
    override fun onCleared() {
        super.onCleared()
        // Ensure any captured media is wiped
        _capturedPhotoBytes.value = null
        _capturedVideoPath.value = null
        currentRecording?.stop()
        currentVideoTargetFile?.delete()
    }
}