package com.vaultaire.android.camera

import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import javax.crypto.SecretKey

/**
 * Test hooks for SecureCameraManager to enable deterministic testing of camera operations.
 * These hooks allow instrumented tests to inject mock CameraX components and verify behavior.
 */
interface SecureCameraTestHooks {

    /**
     * Replaces the real ImageCapture with a mock for photo capture testing.
     * @param mock The mock ImageCapture instance to use
     */
    fun setMockImageCapture(mock: ImageCapture)

    /**
     * Replaces the real VideoCapture with a mock for video recording testing.
     * @param mock The mock VideoCapture<Recorder> instance to use
     */
    fun setMockVideoCapture(mock: VideoCapture<Recorder>)

    /**
     * Replaces the real key provider with a mock for encryption testing.
     * @param provider A function that returns a mock SecretKey
     */
    fun setMockKeyProvider(provider: () -> SecretKey)

    /**
     * Returns the raw bytes of the last captured photo (for verification in tests).
     * Returns null if no photo has been captured or if the buffer was wiped.
     */
    fun captureLastPhotoBytes(): ByteArray?

    /**
     * Returns the file path of the last recorded video .vlt file (for verification in tests).
     * Returns null if no video has been recorded.
     */
    fun captureLastVideoPath(): String?
}
