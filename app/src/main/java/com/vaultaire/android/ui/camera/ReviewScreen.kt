package com.vaultaire.android.ui.camera

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraDimens
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraSecureSurface
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraType
import com.vaultaire.android.ui.camera.CameraViewModel.CaptureMode
import com.vaultaire.android.designsystem.AuroraIconButton as AuroraIconButton2

/**
 * Helper extension to get Activity from any Context.
 */
fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    else -> (this as? android.content.ContextWrapper)?.baseContext?.findActivity()
}

/**
 * Review screen for captured media (photo or video).
 * Shows full-screen preview with Retake/Keep actions.
 * Matches AuroraCameraReview design spec.
 */
@Composable
fun ReviewScreen(
    viewModel: CameraViewModel = viewModel(),
    onRetake: () -> Unit,
    onKeep: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val c = AuroraTheme.colors

    // FLAG_SECURE for review screen too
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val window = activity?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }

    val isPhoto = viewModel.captureMode.value == CaptureMode.PHOTO
    val isVideo = viewModel.captureMode.value == CaptureMode.VIDEO

    // Player for video review
    val player = remember {
        mutableStateOf<ExoPlayer?>(null)
    }

    // Initialize player for video
    LaunchedEffect(viewModel.capturedVideoPath.value) {
        if (isVideo && viewModel.capturedVideoPath.value != null) {
            val newPlayer = ExoPlayer.Builder(context).build()
            val mediaItem = MediaItem.fromUri(viewModel.capturedVideoPath.value!!)
            newPlayer.setMediaItem(mediaItem)
            newPlayer.prepare()
            newPlayer.playWhenReady = false
            player.value = newPlayer
        } else {
            player.value?.release()
            player.value = null
        }
    }

    // Cleanup on dismiss
    DisposableEffect(Unit) {
        onDispose {
            player.value?.release()
            player.value = null
        }
    }

    AuroraSecureSurface(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Media preview - full screen
            if (isPhoto) {
                viewModel.capturedPhotoBytes.value?.let { bytes ->
                    val bitmap = remember(bytes) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }

                    if (bitmap != null) {
                        AndroidView(
                            factory = { context ->
                                android.widget.ImageView(context).apply {
                                    setImageBitmap(bitmap)
                                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                    layoutParams = android.widget.LinearLayout.LayoutParams(
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else if (isVideo) {
                // Video PlayerView
                AndroidPlayerView(
                    player = player.value,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top bar with Retake/Review/Keep
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Retake button (left)
                AuroraIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Retake",
                    onClick = {
                        viewModel.onRetake()
                        onRetake()
                    },
                    onInk = true
                )

                // Title
                Text(
                    text = "Review",
                    style = AuroraType.headlineM,
                    color = c.ink900
                )

                // Keep button (right)
                AuroraIconButton(
                    icon = Icons.Rounded.Check,
                    contentDescription = "Keep",
                    onClick = {
                        viewModel.onKeep()
                        onKeep()
                    },
                    onInk = true,
                    modifier = Modifier.width(44.dp)
                )
            }

            // Bottom glass-raised sheet with context hint and audio badge
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Context hint
                Text(
                    text = if (isPhoto)
                        "Photo will be encrypted into vault"
                    else
                        "Video will be encrypted into vault",
                    style = AuroraType.bodyM,
                    color = c.ink500
                )

                // Audio off badge for video without mic permission
                if (isVideo && !viewModel.hasAudioPermission.value) {
                    AudioOffBadge()
                }
            }
        }
    }
}

/**
 * Audio off badge - butter pill style per design spec.
 */
@Composable
fun AudioOffBadge(modifier: Modifier = Modifier) {
    val c = AuroraTheme.colors
    Box(
        modifier = modifier
            .background(c.tintButter.copy(alpha = 0.92f))
            .clip(AuroraShapes.md)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MicOff,
                contentDescription = null,
                tint = c.tintButterDeep,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Audio off",
                style = AuroraType.labelM,
                color = c.tintButterDeep
            )
        }
    }
}

/**
 * Compose wrapper for ExoPlayer PlayerView.
 */
@Composable
fun AndroidPlayerView(
    player: ExoPlayer?,
    modifier: Modifier = Modifier
) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = modifier,
        update = { playerView ->
            playerView.player = player
        }
    )
}