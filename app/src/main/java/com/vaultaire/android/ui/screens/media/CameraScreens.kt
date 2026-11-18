package com.vaultaire.android.ui.screens.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraChip
import com.vaultaire.android.designsystem.AuroraDimens
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraSecureSurface
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraTint
import com.vaultaire.android.designsystem.AuroraTopBar
import com.vaultaire.android.designsystem.AuroraType
import kotlinx.coroutines.delay

/**
 * Camera capture screen with 76dp shutter, photo/video modes, recording state,
 * and direct encryption into the vault per plan/design-system/flows/03-camera-and-notifications.md.
 */
@Composable
fun CameraCaptureScreen(
    onBack: () -> Unit,
    onPhotoCaptured: () -> Unit,
    onVideoCaptured: () -> Unit,
    onOpenPermission: () -> Unit,
) {
    val c = AuroraTheme.colors
    var isVideoMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var isFrontCamera by remember { mutableStateOf(false) }

    // Recording timer simulation
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    AuroraSecureSurface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Camera Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AuroraDimens.topBar)
                    .padding(horizontal = AuroraDimens.screenPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AuroraIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Close camera",
                    onClick = onBack,
                    onInk = true,
                )

                // Recording Status Indicator (if recording)
                if (isRecording) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(c.danger.copy(alpha = 0.2f))
                            .border(1.dp, c.danger, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(c.danger),
                        )
                        Spacer(Modifier.width(8.dp))
                        val m = recordingSeconds / 60
                        val s = recordingSeconds % 60
                        Text(
                            text = "%02d:%02d".format(m, s),
                            style = AuroraType.monoM,
                            color = c.solid,
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AuroraIconButton(
                            icon = Icons.Filled.FlashOn,
                            contentDescription = "Flash toggle",
                            onClick = { /* simulated flash */ },
                            onInk = true,
                        )
                        AuroraIconButton(
                            icon = Icons.Filled.Security,
                            contentDescription = "Camera permission rationale",
                            onClick = onOpenPermission,
                            onInk = true,
                        )
                    }
                }
            }

            // Viewfinder Surface
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(AuroraShapes.xl)
                    .background(c.surfaceInk.copy(alpha = 0.5f))
                    .border(1.dp, c.border.copy(alpha = 0.2f), AuroraShapes.xl),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isVideoMode) Icons.Filled.Videocam else Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = c.solid.copy(alpha = 0.35f),
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isFrontCamera) "Front Camera" else "Main Camera",
                        style = AuroraType.labelM,
                        color = c.solid.copy(alpha = 0.5f),
                    )
                }
            }

            // Bottom Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                AuroraChip(
                    label = "Photo",
                    selected = !isVideoMode,
                    onClick = {
                        if (!isRecording) isVideoMode = false
                    },
                )
                Spacer(Modifier.width(12.dp))
                AuroraChip(
                    label = "Video",
                    selected = isVideoMode,
                    onClick = {
                        if (!isRecording) isVideoMode = true
                    },
                )
            }

            // Bottom Controls Bar with 76dp Shutter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: Spacer or thumbnail placeholder
                Box(modifier = Modifier.size(48.dp))

                // Center: 76dp Shutter Button
                Box(
                    modifier = Modifier
                        .size(AuroraDimens.shutter)
                        .clip(CircleShape)
                        .background(c.solid.copy(alpha = 0.15f))
                        .border(3.dp, c.solid, CircleShape)
                        .clickable {
                            if (!isVideoMode) {
                                onPhotoCaptured()
                            } else {
                                if (isRecording) {
                                    isRecording = false
                                    onVideoCaptured()
                                } else {
                                    isRecording = true
                                }
                            }
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(if (isRecording) AuroraShapes.xs else CircleShape)
                            .background(if (isVideoMode) c.danger else c.solid),
                    )
                }

                // Right: Flip Camera Button
                AuroraIconButton(
                    icon = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch camera",
                    onClick = { isFrontCamera = !isFrontCamera },
                    onInk = true,
                )
            }
        }
    }
}

/**
 * Review captured photo before encrypting directly to vault.
 */
@Composable
fun CameraReviewPhotoScreen(
    onBack: () -> Unit,
    onKeep: () -> Unit,
) {
    val c = AuroraTheme.colors
    var isEncrypting by remember { mutableStateOf(false) }

    AuroraSecureSurface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            AuroraTopBar(
                title = "Review photo",
                eyebrow = "Secure Capture",
                leading = {
                    AuroraIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        onInk = true,
                    )
                },
            )

            // Preview Frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clip(AuroraShapes.lg)
                    .background(c.surfaceInk.copy(alpha = 0.4f))
                    .border(1.dp, c.border.copy(alpha = 0.2f), AuroraShapes.lg),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = c.solid.copy(alpha = 0.8f),
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "EXIF stripped · 3.8 MB",
                        style = AuroraType.monoM,
                        color = c.solid,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Location and metadata removed before encryption",
                        style = AuroraType.bodyS,
                        color = c.solid.copy(alpha = 0.6f),
                    )
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AuroraDimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AuroraButton(
                    text = "Retake",
                    onClick = onBack,
                    variant = AuroraButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                AuroraButton(
                    text = if (isEncrypting) "Encrypting..." else "Keep",
                    loading = isEncrypting,
                    onClick = {
                        isEncrypting = true
                        onKeep()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Review captured video with playback transport before keeping.
 */
@Composable
fun CameraReviewVideoScreen(
    onBack: () -> Unit,
    onKeep: () -> Unit,
) {
    val c = AuroraTheme.colors
    var isPlaying by remember { mutableStateOf(false) }

    AuroraSecureSurface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            AuroraTopBar(
                title = "Review video",
                eyebrow = "Secure Capture",
                leading = {
                    AuroraIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        onInk = true,
                    )
                },
            )

            // Video Preview Surface
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clip(AuroraShapes.lg)
                    .background(c.surfaceInk.copy(alpha = 0.45f))
                    .border(1.dp, c.border.copy(alpha = 0.2f), AuroraShapes.lg),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(c.solid.copy(alpha = 0.25f))
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = c.solid,
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Bottom Duration Readout
                Text(
                    text = "0:12 · About 18.4 MB",
                    style = AuroraType.monoM,
                    color = c.solid,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }

            // Bottom Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AuroraDimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AuroraButton(
                    text = "Retake",
                    onClick = onBack,
                    variant = AuroraButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                AuroraButton(
                    text = "Keep",
                    onClick = onKeep,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Camera permission rationale screen. Never a dead end per spec.
 */
@Composable
fun CameraPermissionScreen(
    onBack: () -> Unit,
    onGranted: () -> Unit,
) {
    val c = AuroraTheme.colors
    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            AuroraTopBar(
                title = "Camera access",
                eyebrow = "Permission Rationale",
                leading = {
                    AuroraIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(c.tintLilac),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = c.iris600,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Camera access required",
                    style = AuroraType.headlineM,
                    color = c.ink900,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Vaultaire never uploads camera images without an action you take. Photos and videos are encrypted directly into your vault with zero unencrypted cache.",
                    style = AuroraType.bodyM,
                    color = c.ink600,
                )
                Spacer(Modifier.height(32.dp))
                AuroraButton(
                    text = "Allow camera",
                    onClick = onGranted,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                AuroraButton(
                    text = "Not now",
                    onClick = onBack,
                    variant = AuroraButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
