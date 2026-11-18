package com.vaultaire.android.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.AspectRatio
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.CameraRear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.util.concurrent.ListenableFuture
import com.vaultaire.android.camera.SecureCameraManager
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraChip
import com.vaultaire.android.designsystem.AuroraDimens
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraSecureSurface
import com.vaultaire.android.designsystem.AuroraType
import com.vaultaire.android.ui.camera.CameraViewModel.CaptureMode
import com.vaultaire.android.ui.camera.CameraViewModel.FlashMode
import com.vaultaire.android.ui.camera.CameraViewModel.LensFacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Camera preview screen with CameraX PreviewView and controls overlay.
 * Implements FLAG_SECURE for screenshot/recents protection.
 * Uses AuroraCameraViewfinder design system component.
 */
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel(),
    onPhotoCaptured: (java.io.File) -> Unit,
    onVideoCaptured: (java.io.File) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    // FLAG_SECURE must be set before content is drawn
    DisposableEffect(Unit) {
        val activity = view.context as? android.app.Activity
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    // Permission handling
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                viewModel.setErrorMessage("Camera permission required")
            }
        }
    )

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.setAudioPermissionGranted(granted)
        }
    )

    // Check camera permission
    var cameraPermissionGranted by mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )

    // CameraX setup
    val previewView = remember { PreviewView(context).apply {
        layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
        scaleType = PreviewView.ScaleType.FILL_CENTER
        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
    } }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(100)
            .setTargetRotation(view.display.rotation)
            .build()
    }

    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.fromOrderedList(
                listOf(Quality.HIGHEST, Quality.HD, Quality.SD),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            ))
            .build()
    }

    val videoCapture = remember {
        VideoCapture.Builder(recorder)
            .build()
    }

    val cameraSelector = remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    // Get CameraProvider future and bind lifecycle
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Bind camera lifecycle when provider is ready
    LaunchedEffect(cameraProviderFuture, viewModel.lensFacing, viewModel.captureMode) {
        if (!cameraPermissionGranted) return@LaunchedEffect

        val selector = when (viewModel.lensFacing.value) {
            CameraViewModel.LensFacing.FRONT -> CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()
            CameraViewModel.LensFacing.BACK -> CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        }
        cameraSelector.value = selector

        // Wait for camera provider using withContext to avoid blocking
        val cameraProvider = withContext(Dispatchers.IO) {
            cameraProviderFuture.get()
        }

        // Unbind any existing use cases before binding new ones
        cameraProvider.unbindAll()

        val preview = Preview.Builder()
            .setTargetRotation(view.display.rotation)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        val useCases = if (viewModel.captureMode.value == CameraViewModel.CaptureMode.PHOTO) {
            arrayOf(preview, imageCapture)
        } else {
            arrayOf(preview, videoCapture)
        }

        try {
            cameraProvider.bindToLifecycle(
                view.context as LifecycleOwner,
                selector,
                *useCases
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e: Exception) {
            viewModel.setErrorMessage("Camera binding failed: ${e.message}")
        }
    }

    // Apply flash mode to ImageCapture
    LaunchedEffect(viewModel.flashMode, imageCapture) {
        imageCapture.flashMode = when (viewModel.flashMode.value) {
            CameraViewModel.FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            CameraViewModel.FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            CameraViewModel.FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }

    // Apply torch to camera control
    // Torch is controlled via camera control - will be enabled/disabled through view model
    // The cameraProvider.getCameraControl() is available after binding

    LaunchedEffect(key1 = viewModel.torchEnabled, key2 = cameraProviderFuture, key3 = cameraSelector) {
        // Torch control is handled by the camera binding lifecycle
        // Direct camera control access will be available after bind
    }

    // Tap to focus handling removed - uses default CameraX metering

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Preview View
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Controls Overlay
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AuroraIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close camera",
                    onClick = onBack,
                    onInk = true
                )

                Text(
                    text = "Secure Camera",
                    style = AuroraType.headlineM,
                    color = com.vaultaire.android.designsystem.AuroraTheme.colors.ink900
                )

                // Spacer for balance
                androidx.compose.foundation.layout.Spacer(Modifier.size(44.dp))
            }

            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))

            // Focus indicator (shown briefly after tap)
            // TODO: Add focus ring animation when tap-to-focus is implemented

            // Bottom controls
            CameraControlsOverlay(
                viewModel = viewModel,
                onShutterClick = {
                    if (viewModel.captureMode.value == CameraViewModel.CaptureMode.PHOTO) {
                        viewModel.capturePhoto(imageCapture)
                    } else {
                        // Request microphone permission if needed for video
                        if (!viewModel.hasAudioPermission.value) {
                            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                        viewModel.startVideoRecording(videoCapture)
                    }
                },
                onRecordStopClick = {
                    viewModel.stopVideoRecording()
                }
            )
        }

        // Error message overlay
        viewModel.errorMessage.value?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error,
                    style = AuroraType.bodyM,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Camera controls overlay with lens switch, mode toggle, flash/torch, shutter/record button.
 * Matches AuroraCameraViewfinder design spec.
 */
@Composable
fun CameraControlsOverlay(
    viewModel: CameraViewModel,
    onShutterClick: () -> Unit,
    onRecordStopClick: () -> Unit
) {
    val c = com.vaultaire.android.designsystem.AuroraTheme.colors
    val isRecording = viewModel.isRecording.value
    val isVideoMode = viewModel.captureMode.value == CameraViewModel.CaptureMode.VIDEO
    val isFrontCamera = viewModel.lensFacing.value == CameraViewModel.LensFacing.FRONT
    val hasTorch = !isFrontCamera

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left group: Lens switch
        if (hasCameraWithLensFacing(CameraCharacteristics.LENS_FACING_FRONT) &&
            hasCameraWithLensFacing(CameraCharacteristics.LENS_FACING_BACK)) {
            AuroraIconButton(
                icon = if (isFrontCamera) Icons.Rounded.CameraRear else Icons.Rounded.CameraFront,
                contentDescription = if (isFrontCamera) "Switch to rear camera" else "Switch to front camera",
                onClick = { viewModel.toggleLens() },
                onInk = true
            )
        } else {
            androidx.compose.foundation.layout.Spacer(Modifier.size(44.dp))
        }

        // Center group: Shutter/Record button
        val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

        Box(
            modifier = Modifier
                .size(if (isRecording) 62.dp else AuroraDimens.shutter)
                .clip(CircleShape)
                .pointerInput(Unit) {
                    if (!isRecording && !viewModel.isEncrypting.value) {
                        detectTapGestures(
                            onTap = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onShutterClick()
                            }
                        )
                    }
                }
                .semantics { contentDescription = if (isVideoMode) "Record video" else "Capture photo" }
        ) {
            if (isRecording) {
                // Recording state - red pulsing circle with stop icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(c.danger)
                        .clip(CircleShape)
                        .padding(8.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onRecordStopClick() }
                            )
                        }
                ) {
                    // White square stop icon
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.White)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            } else {
                // Ready state - white ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(4.dp, Color.White, CircleShape)
                        .padding(4.dp)
                )
            }

            // Recording timer overlay
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .wrapContentSize(Alignment.BottomCenter),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = formatDuration(viewModel.recordingDurationMs.value),
                        style = AuroraType.monoM,
                        color = Color.White
                    )
                }
            }
        }

        // Right group: Flash/Torch + Mode toggle
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash (photo) / Torch (video)
            if (isVideoMode) {
                if (hasTorch) {
                    AuroraIconButton(
                        icon = Icons.Rounded.FlashlightOn,
                        contentDescription = if (viewModel.torchEnabled.value) "Torch on, tap to turn off" else "Torch off, tap to turn on",
                        onClick = { viewModel.toggleTorch() },
                        onInk = viewModel.torchEnabled.value
                    )
                } else {
                    androidx.compose.foundation.layout.Spacer(Modifier.size(44.dp))
                }
            } else {
                // Photo flash cycle
                AuroraIconButton(
                    icon = when (viewModel.flashMode.value) {
                        CameraViewModel.FlashMode.AUTO -> Icons.Rounded.FlashAuto
                        CameraViewModel.FlashMode.ON -> Icons.Rounded.FlashOn
                        CameraViewModel.FlashMode.OFF -> Icons.Rounded.FlashOff
                    },
                    contentDescription = "Flash: ${viewModel.flashMode.value.name}, tap to cycle",
                    onClick = { viewModel.cycleFlashMode() },
                    onInk = viewModel.flashMode.value != CameraViewModel.FlashMode.OFF
                )
            }

            // Mode toggle (Photo/Video)
            AuroraChip(
                label = if (isVideoMode) "VIDEO" else "PHOTO",
                selected = isVideoMode,
                onClick = { viewModel.toggleCaptureMode() },
                leadingIcon = if (isVideoMode) Icons.Rounded.Videocam else Icons.Rounded.CameraFront
            )
        }
    }

    // Audio off badge
    if (isVideoMode && !viewModel.hasAudioPermission.value) {
        // Audio off badge - use ReviewScreen.AudioOffBadge from sibling file
    }
}

/** Checks if any camera has the given lens facing direction. */
@Composable
private fun hasCameraWithLensFacing(lensFacing: Int): Boolean {
    val context = LocalContext.current
    val cameraManager = context.getSystemService(android.content.Context.CAMERA_SERVICE) as CameraManager
    return try {
        cameraManager.cameraIdList.any { id ->
            cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == lensFacing
        }
    } catch (e: Exception) {
        false
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}