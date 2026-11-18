/**
 * Vaultaire Aurora — core Compose primitives.
 *
 * Reference implementation for the design system. Adopt selectively; every
 * component must be screenshot-tested in Glass, Solid, and Night.
 */
package com.vaultaire.android.designsystem

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.hypot

// ---------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------

enum class AuroraButtonVariant { Primary, Secondary, Ghost, Danger, Pro }

@Composable
fun AuroraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AuroraButtonVariant = AuroraButtonVariant.Primary,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val c = AuroraTheme.colors
    val mode = AuroraTheme.mode.surface
    val (bg, fg, border) = when (variant) {
        AuroraButtonVariant.Primary -> Triple(c.surfaceInk, c.solid, null)
        AuroraButtonVariant.Secondary -> Triple(c.raisedBackground(mode), c.ink900, c.border)
        AuroraButtonVariant.Ghost -> Triple(Color.Transparent, c.ink700, null)
        AuroraButtonVariant.Danger -> Triple(c.dangerSoft, c.danger, c.tintRose.copy(alpha = 0.7f))
        AuroraButtonVariant.Pro -> Triple(c.surfaceInk, c.solid, null)
    }

    Row(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(if (enabled) bg else bg.copy(alpha = 0.38f))
            .then(if (border != null) Modifier.border(1.dp, border, CircleShape) else Modifier)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = fg,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
        } else if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = AuroraType.labelL,
            color = if (enabled) fg else fg.copy(alpha = 0.7f),
        )
    }
}

/**
 * Aurora icon button complying with Aurora design tokens and 48dp minimum touch target.
 */
@Composable
fun AuroraIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onInk: Boolean = false,
) {
    val c = AuroraTheme.colors
    Box(
        modifier = modifier
            .sizeIn(minWidth = AuroraDimens.touchTarget, minHeight = AuroraDimens.touchTarget)
            .clip(CircleShape)
            .background(if (onInk) c.surfaceInk.copy(alpha = 0.62f) else c.raisedBackground(AuroraTheme.mode.surface))
            .then(if (!onInk) Modifier.border(1.dp, c.border, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (onInk) Color.White else c.ink700,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Chips
// ---------------------------------------------------------------------------

/**
 * Aurora chip with interactive clickable surface meeting 48dp minimum touch target.
 */
@Composable
fun AuroraChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val c = AuroraTheme.colors
    Box(
        modifier = modifier
            .sizeIn(minWidth = AuroraDimens.touchTarget, minHeight = AuroraDimens.touchTarget)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(CircleShape)
                .background(if (selected) c.surfaceInk else c.cardBackground(AuroraTheme.mode.surface))
                .then(if (!selected) Modifier.border(1.dp, c.border, CircleShape) else Modifier)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = if (selected) c.solid else c.ink700, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                style = AuroraType.labelL,
                color = if (selected) c.solid else c.ink700,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Switches & Toggles
// ---------------------------------------------------------------------------

/**
 * Aurora switch control complying with Aurora design tokens and 48dp minimum touch target.
 *
 * @param checked Whether the switch is currently on or off.
 * @param onCheckedChange Callback invoked when the switch state changes, or null if read-only.
 * @param modifier Modifier to be applied to the switch.
 * @param enabled Whether the switch is enabled for user interaction.
 */
@Composable
fun AuroraSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = AuroraTheme.colors
    val offset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        label = "aurora_switch_thumb",
    )
    val trackColor = when {
        !enabled -> (if (checked) c.iris600 else c.ink200).copy(alpha = 0.38f)
        checked -> c.iris600
        else -> c.ink200
    }
    val thumbColor = when {
        !enabled -> Color.White.copy(alpha = 0.6f)
        checked -> c.solid
        else -> Color.White
    }

    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .sizeIn(minWidth = AuroraDimens.touchTarget, minHeight = AuroraDimens.touchTarget)
            .then(toggleModifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(CircleShape)
                .background(trackColor),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(22.dp)
                    .offset(x = (20 * offset).dp)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .shadow(elevation = if (enabled) 2.dp else 0.dp, shape = CircleShape),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Cards
// ---------------------------------------------------------------------------

enum class AuroraTint { None, Lilac, Rose, Peach, Mint, Sky, Butter, Sand }

@Composable
fun AuroraGlassCard(
    modifier: Modifier = Modifier,
    tint: AuroraTint = AuroraTint.None,
    raised: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val c = AuroraTheme.colors
    val tintBg = when (tint) {
        AuroraTint.None -> null
        AuroraTint.Lilac -> c.tintLilac
        AuroraTint.Rose -> c.tintRose
        AuroraTint.Peach -> c.tintPeach
        AuroraTint.Mint -> c.tintMint
        AuroraTint.Sky -> c.tintSky
        AuroraTint.Butter -> c.tintButter
        AuroraTint.Sand -> c.tintSand
    }
    val base = Modifier
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    AuroraGlassSurface(
        modifier = modifier.then(base),
        raised = raised,
        content = {
            if (tintBg != null) {
                Box(Modifier.matchParentSize().background(tintBg.copy(alpha = 0.92f)))
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.padding(AuroraDimens.cardPadding),
                content = content,
            )
        },
    )
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@Composable
fun AuroraTopBar(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().height(AuroraDimens.topBar).padding(horizontal = AuroraDimens.screenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(eyebrow, style = AuroraType.overline, color = AuroraTheme.colors.ink500)
            }
            Text(title, style = AuroraType.headlineM, color = AuroraTheme.colors.ink900)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

// ---------------------------------------------------------------------------
// Bottom navigation
// ---------------------------------------------------------------------------

data class AuroraNavItem(val label: String, val icon: ImageVector)

@Composable
fun AuroraNavBar(
    items: List<AuroraNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AuroraTheme.colors
    AuroraGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AuroraDimens.navInset)
            .height(AuroraDimens.navBar)
            .shadow(22.dp, AuroraShapes.xxxl),
        shape = AuroraShapes.xxxl,
        raised = true,
        content = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                items.forEachIndexed { index, item ->
                    val active = index == selectedIndex
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { onSelect(index) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Row(
                            modifier = Modifier
                                .then(if (active) Modifier.clip(CircleShape).background(c.surfaceInk).padding(horizontal = 18.dp, vertical = 8.dp) else Modifier.padding(vertical = 8.dp)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = if (active) c.solid else c.ink600,
                                modifier = Modifier.size(22.dp),
                            )
                            if (active) {
                                Spacer(Modifier.width(8.dp))
                                Text(item.label, style = AuroraType.labelM, color = c.solid)
                            }
                        }
                    }
                }
            }
        },
    )
}

// ---------------------------------------------------------------------------
// Stats and progress
// ---------------------------------------------------------------------------

@Composable
fun AuroraStat(value: String, label: String, modifier: Modifier = Modifier, dark: Boolean = false) {
    val c = AuroraTheme.colors
    Column(modifier) {
        Text(value, style = AuroraType.monoL, color = if (dark) c.solid else c.ink900)
        Text(label, style = AuroraType.labelM, color = if (dark) c.solid.copy(alpha = 0.72f) else c.ink500)
    }
}

@Composable
fun AuroraProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 120.dp,
    color: Color? = null,
    track: Color? = null,
    center: @Composable () -> Unit = {},
) {
    val c = AuroraTheme.colors
    val p = progress.coerceIn(0f, 1f)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = size.toPx() * 0.09f
            drawArc(
                color = track ?: c.ink100,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color ?: c.iris600,
                startAngle = -90f,
                sweepAngle = 360f * p,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        center()
    }
}

// ---------------------------------------------------------------------------
// Secure wrapper
// ---------------------------------------------------------------------------

private var secureSurfaceCount = 0

/**
 * Applies FLAG_SECURE and a solid background. Use on every screen that shows
 * credentials, media, camera preview, or an editable pattern.
 *
 * Employs reference counting so transitions between consecutive secure screens
 * do not prematurely clear FLAG_SECURE on the host window.
 */
@Composable
fun AuroraSecureSurface(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val c = AuroraTheme.colors
    val view = LocalView.current
    DisposableEffect(Unit) {
        val activity = view.context.findActivity()
        val window = activity?.window
        if (secureSurfaceCount == 0) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        secureSurfaceCount++
        onDispose {
            secureSurfaceCount = (secureSurfaceCount - 1).coerceAtLeast(0)
            if (secureSurfaceCount == 0) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
    Box(
        modifier = modifier.background(c.solidRaised),
        content = content,
    )
}

// ---------------------------------------------------------------------------
// Pattern grid
// ---------------------------------------------------------------------------

enum class PatternFeedback {
    /** Unlock default: known, unknown, and duress all resolve identically. */
    Neutral,

    /** Pattern creation only: too few dots / no turn. */
    CreationError,
}

/**
 * 5x5 pattern grid. Unlock feedback is intentionally neutral — do not add a
 * "wrong pattern" state here. The only error variant is for creation.
 */
@Composable
fun AuroraPatternGrid(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    feedback: PatternFeedback = PatternFeedback.Neutral,
    onPatternComplete: (List<Int>) -> Unit = {},
) {
    val c = AuroraTheme.colors
    val haptics = LocalHapticFeedback.current
    val connected = remember { mutableStateListOf<Int>() }
    var active by remember { mutableStateOf<Int?>(null) }
    val activeColor = if (feedback == PatternFeedback.CreationError) c.danger else c.iris600

    Canvas(
        modifier = modifier
            .semantics {
                contentDescription = "Pattern grid, 5 rows, 5 columns"
            }
            .pointerInput(enabled, feedback) {
                if (!enabled) return@pointerInput
                fun dotAt(position: Offset): Int? {
                    val cell = size.width / 5f
                    val col = (position.x / cell).toInt()
                    val row = (position.y / cell).toInt()
                    if (row !in 0..4 || col !in 0..4) return null
                    val cx = (col + 0.5f) * cell
                    val cy = (row + 0.5f) * cell
                    val distance = hypot(position.x - cx, position.y - cy)
                    return if (distance <= cell * 0.46f) row * 5 + col else null
                }

                fun add(target: Int) {
                    if (connected.contains(target)) return
                    val from = connected.lastOrNull()
                    if (from != null) {
                        connected.addAll(intermediateDots(from, target).filterNot { connected.contains(it) })
                    }
                    connected.add(target)
                    active = target
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                awaitEachGesture {
                    val down = awaitFirstDown()
                    val startDot = dotAt(down.position)
                    if (startDot != null) {
                        add(startDot)
                    }
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.changedToUp()) {
                            change.consume()
                            break
                        }
                        val currentDot = dotAt(change.position)
                        if (currentDot != null && currentDot != active) {
                            moved = true
                            add(currentDot)
                        }
                    }
                    active = null
                    val result = connected.toList()
                    if (moved) {
                        connected.clear()
                    }
                    onPatternComplete(result)
                }
            },
    ) {
        val cell = size.width / 5f
        val dotRadius = cell * 0.21f

        fun centerOf(index: Int) = Offset(
            x = (index % 5 + 0.5f) * cell,
            y = (index / 5 + 0.5f) * cell,
        )

        // Connecting line
        if (connected.size > 1) {
            for (i in 0 until connected.lastIndex) {
                drawLine(
                    color = activeColor,
                    start = centerOf(connected[i]),
                    end = centerOf(connected[i + 1]),
                    strokeWidth = cell * 0.054f,
                    cap = StrokeCap.Round,
                )
            }
            if (active != null) {
                drawLine(
                    color = activeColor.copy(alpha = 0.55f),
                    start = centerOf(connected.last()),
                    end = centerOf(active!!),
                    strokeWidth = cell * 0.054f,
                    cap = StrokeCap.Round,
                )
            }
        }

        for (index in 0 until 25) {
            val center = centerOf(index)
            val isConnected = connected.contains(index)
            val isActive = active == index
            if (isConnected || isActive) {
                drawCircle(activeColor.copy(alpha = 0.22f), radius = dotRadius * 1.7f, center = center)
            }
            drawCircle(
                color = when {
                    isActive -> activeColor
                    isConnected -> activeColor
                    else -> c.ink200
                },
                radius = when {
                    isActive -> dotRadius * 1.18f
                    isConnected -> dotRadius * 1.10f
                    else -> dotRadius
                },
                center = center,
            )
            if (!isConnected && !isActive) {
                drawCircle(
                    color = c.ink300.copy(alpha = 0.6f),
                    radius = dotRadius * 0.94f,
                    center = center,
                    style = Stroke(width = cell * 0.018f),
                )
            }
        }
    }
}

private fun intermediateDots(from: Int, to: Int): List<Int> {
    val r1 = from / 5
    val c1 = from % 5
    val r2 = to / 5
    val c2 = to % 5
    val dr = r2 - r1
    val dc = c2 - c1
    val steps = gcd(abs(dr), abs(dc))
    if (steps <= 1) return emptyList()
    if (dr != 0 && dc != 0 && abs(dr) != abs(dc)) return emptyList()
    return (1 until steps).map { step ->
        (r1 + dr / steps * step) * 5 + (c1 + dc / steps * step)
    }
}

private fun gcd(a: Int, b: Int): Int {
    var x = a
    var y = b
    while (y != 0) {
        val t = y
        y = x % y
        x = t
    }
    return x
}

// ---------------------------------------------------------------------------
// Recovery / share phrase grid
// ---------------------------------------------------------------------------

/**
 * Solid, FLAG_SECURE word grid. Never place this on a glass card.
 * Word count is 9 (recovery), 12 (share), or custom.
 */
@Composable
fun AuroraPhraseGrid(
    words: List<String>,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    val c = AuroraTheme.colors
    AuroraSecureSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            words.withIndex().chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (index, word) ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(AuroraShapes.md)
                                .background(c.solid)
                                .border(1.dp, c.hairline, AuroraShapes.md)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = AuroraType.monoS,
                                color = c.ink400,
                            )
                            Text(
                                text = word,
                                style = AuroraType.monoM.copy(fontSize = 22.sp, lineHeight = 30.sp),
                                color = c.ink900,
                            )
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
fun AuroraEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val c = AuroraTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(c.iris100)
                .border(1.dp, c.iris200, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = c.iris600, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = AuroraType.headlineM, color = c.ink900, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = AuroraType.bodyM, color = c.ink600, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        AuroraButton(primaryLabel, onPrimary)
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(8.dp))
            AuroraButton(secondaryLabel, onSecondary, variant = AuroraButtonVariant.Ghost)
        }
    }
}
