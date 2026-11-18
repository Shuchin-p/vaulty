/**
 * Vaultaire Aurora — Compose theme reference implementation.
 *
 * This file is a design-system artifact, not production wiring. Move it to
 * `app/src/main/java/com/vaultaire/android/designsystem/` when adopting.
 *
 * Rules:
 *  - No component may hardcode a color, size, or radius.
 *  - Glass is a finish; every surface must also render in Solid mode.
 *  - Bundle Instrument Sans + Roboto Mono in res/font. The fallbacks below
 *    keep the theme compilable before fonts are added.
 */
package com.vaultaire.android.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas

// ---------------------------------------------------------------------------
// Modes
// ---------------------------------------------------------------------------

enum class AuroraSurfaceMode { Glass, Solid }

@Immutable
data class AuroraMode(
    val surface: AuroraSurfaceMode = AuroraSurfaceMode.Glass,
    val night: Boolean = false,
) {
    companion object {
        /** Reduce transparency, battery saver, and API < 31 all resolve to Solid. */
        fun auto(night: Boolean, reduceTransparency: Boolean): AuroraMode {
            val solid = reduceTransparency || Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            return AuroraMode(if (solid) AuroraSurfaceMode.Solid else AuroraSurfaceMode.Glass, night)
        }
    }
}

// ---------------------------------------------------------------------------
// Color
// ---------------------------------------------------------------------------

@Immutable
data class AuroraColors(
    val ink900: Color,
    val ink800: Color,
    val ink700: Color,
    val ink600: Color,
    val ink500: Color,
    val ink400: Color,
    val ink300: Color,
    val ink200: Color,
    val ink100: Color,
    val ink050: Color,

    val canvasBase: Color,
    val canvasTop: Color,
    val canvasBottom: Color,
    val canvasOrbit: Color,
    val canvasVignette: Color,
    val scrim: Color,

    val glass: Color,
    val glassRaised: Color,
    val glassSubtle: Color,
    val solid: Color,
    val solidRaised: Color,
    val surfaceInk: Color,
    val border: Color,
    val hairline: Color,

    val iris050: Color,
    val iris100: Color,
    val iris200: Color,
    val iris300: Color,
    val iris400: Color,
    val iris500: Color,
    val iris600: Color,
    val iris700: Color,

    val tintLilac: Color,
    val tintLilacDeep: Color,
    val tintRose: Color,
    val tintRoseDeep: Color,
    val tintPeach: Color,
    val tintPeachDeep: Color,
    val tintMint: Color,
    val tintMintDeep: Color,
    val tintSky: Color,
    val tintSkyDeep: Color,
    val tintButter: Color,
    val tintButterDeep: Color,
    val tintSand: Color,
    val tintSandDeep: Color,

    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val focus: Color,

    val gradientHero: List<Color>,
    val gradientDawn: List<Color>,
) {
    /** Card background by surface mode. */
    fun cardBackground(mode: AuroraSurfaceMode): Color =
        if (mode == AuroraSurfaceMode.Glass) glass else solid

    fun raisedBackground(mode: AuroraSurfaceMode): Color =
        if (mode == AuroraSurfaceMode.Glass) glassRaised else solidRaised
}

private val LightInk = listOf(
    Color(0xFF17151F), Color(0xFF24212E), Color(0xFF3A3647), Color(0xFF4A4557),
    Color(0xFF6B6578), Color(0xFF8C8799), Color(0xFFADA8B8), Color(0xFFCFCBD8),
    Color(0xFFE7E4EE), Color(0xFFF4F2F8),
)

private val NightInk = listOf(
    Color(0xFFF4F1FA), Color(0xFFE3DFEC), Color(0xFFCFC9DB), Color(0xFFC3BDD2),
    Color(0xFFA69FB6), Color(0xFF8B849C), Color(0xFF6F687F), Color(0xFF4A4458),
    Color(0xFF302B3D), Color(0xFF221E2E),
)

private fun ink(light: Boolean, i: Int) = if (light) LightInk[i] else NightInk[i]

val AuroraLightColors = AuroraColors(
    ink900 = ink(true, 0), ink800 = ink(true, 1), ink700 = ink(true, 2),
    ink600 = ink(true, 3), ink500 = ink(true, 4), ink400 = ink(true, 5),
    ink300 = ink(true, 6), ink200 = ink(true, 7), ink100 = ink(true, 8), ink050 = ink(true, 9),
    canvasBase = Color(0xFFC9C3E6), canvasTop = Color(0xFFD9D4F0), canvasBottom = Color(0xFFBCB4DD),
    canvasOrbit = Color.White.copy(alpha = 0.30f), canvasVignette = Color(0x1A56408C),
    scrim = Color(0x5217151F),
    glass = Color.White.copy(alpha = 0.62f), glassRaised = Color.White.copy(alpha = 0.80f),
    glassSubtle = Color.White.copy(alpha = 0.42f),
    solid = Color(0xFFF7F5FB), solidRaised = Color(0xFFFFFFFF), surfaceInk = Color(0xFF17151F),
    border = Color.White.copy(alpha = 0.62f), hairline = Color(0x1417151F),
    iris050 = Color(0xFFF3F0FF), iris100 = Color(0xFFE7E2FF), iris200 = Color(0xFFCFC6FF),
    iris300 = Color(0xFFB4A8FF), iris400 = Color(0xFF9A8CF7), iris500 = Color(0xFF7C6BF2),
    iris600 = Color(0xFF6A58E8), iris700 = Color(0xFF5744C4),
    tintLilac = Color(0xFFE9E3FB), tintLilacDeep = Color(0xFF4E3F91),
    tintRose = Color(0xFFF6DDE2), tintRoseDeep = Color(0xFF8E3347),
    tintPeach = Color(0xFFF8E3D3), tintPeachDeep = Color(0xFF8A4B23),
    tintMint = Color(0xFFDDF3D8), tintMintDeep = Color(0xFF2A6B45),
    tintSky = Color(0xFFDCE8FA), tintSkyDeep = Color(0xFF2C5B93),
    tintButter = Color(0xFFF8EFC4), tintButterDeep = Color(0xFF7A5D0C),
    tintSand = Color(0xFFF2EDE3), tintSandDeep = Color(0xFF6B5B43),
    success = Color(0xFF2A6B45), successSoft = Color(0xFFDDF3D8),
    warning = Color(0xFF7A5D0C), warningSoft = Color(0xFFF8EFC4),
    danger = Color(0xFF8E3347), dangerSoft = Color(0xFFF6DDE2),
    info = Color(0xFF2C5B93), infoSoft = Color(0xFFDCE8FA),
    focus = Color(0xFF6A58E8),
    gradientHero = listOf(Color(0xFFD8CCFF), Color(0xFFE9DFF5), Color(0xFFF5D9E4)),
    gradientDawn = listOf(Color(0xFFF6DDE2), Color(0xFFF8E3D3), Color(0xFFF8EFC4)),
)

val AuroraNightColors = AuroraLightColors.copy(
    ink900 = ink(false, 0), ink800 = ink(false, 1), ink700 = ink(false, 2),
    ink600 = ink(false, 3), ink500 = ink(false, 4), ink400 = ink(false, 5),
    ink300 = ink(false, 6), ink200 = ink(false, 7), ink100 = ink(false, 8), ink050 = ink(false, 9),
    canvasBase = Color(0xFF14121C), canvasTop = Color(0xFF1B1728), canvasBottom = Color(0xFF0F0D16),
    canvasOrbit = Color.White.copy(alpha = 0.06f), canvasVignette = Color(0x66000000),
    scrim = Color(0x8F000000),
    glass = Color(0xFF282338).copy(alpha = 0.58f), glassRaised = Color(0xFF322C46).copy(alpha = 0.76f),
    glassSubtle = Color(0xFF282338).copy(alpha = 0.36f),
    solid = Color(0xFF1B1826), solidRaised = Color(0xFF242031), surfaceInk = Color(0xFFF4F1FA),
    border = Color.White.copy(alpha = 0.14f), hairline = Color.White.copy(alpha = 0.08f),
    iris050 = Color(0xFF1E1938), iris100 = Color(0xFF2A2350), iris200 = Color(0xFF3A3170),
    iris300 = Color(0xFF5A4DA8), iris400 = Color(0xFF9A8CF7), iris500 = Color(0xFFA192FF),
    iris600 = Color(0xFFB4A8FF), iris700 = Color(0xFFCFC6FF),
    tintLilac = Color(0xFF2A2342), tintLilacDeep = Color(0xFFCFC6FF),
    tintRose = Color(0xFF3A2028), tintRoseDeep = Color(0xFFFF9DA8),
    tintPeach = Color(0xFF3E2A1D), tintPeachDeep = Color(0xFFF2CBB0),
    tintMint = Color(0xFF1E3A2C), tintMintDeep = Color(0xFF8FE0AC),
    tintSky = Color(0xFF1E2E44), tintSkyDeep = Color(0xFF9AC4FF),
    tintButter = Color(0xFF3A3218), tintButterDeep = Color(0xFFF2CE7A),
    tintSand = Color(0xFF322D26), tintSandDeep = Color(0xFFD9CBB4),
    success = Color(0xFF8FE0AC), successSoft = Color(0xFF1E3A2C),
    warning = Color(0xFFF2CE7A), warningSoft = Color(0xFF3A3218),
    danger = Color(0xFFFF9DA8), dangerSoft = Color(0xFF3A2028),
    info = Color(0xFF9AC4FF), infoSoft = Color(0xFF1E2E44),
    focus = Color(0xFFB4A8FF),
    gradientHero = listOf(Color(0xFF3B3260), Color(0xFF2C2547), Color(0xFF3A2533)),
    gradientDawn = listOf(Color(0xFF3A2028), Color(0xFF3E2A1D), Color(0xFF3A3218)),
)

// ---------------------------------------------------------------------------
// Type
// ---------------------------------------------------------------------------

private val Display = FontFamily.SansSerif      // TODO: Instrument Sans
private val Text = FontFamily.SansSerif         // TODO: Instrument Sans
private val Mono = FontFamily.Monospace         // TODO: Roboto Mono

val AuroraTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-0.04).em),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.035).em),
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.03).em),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.015).em),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em),
    titleMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Text, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Text, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Text, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.01.em),
    labelMedium = TextStyle(fontFamily = Text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em),
    labelSmall = TextStyle(fontFamily = Text, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.12.em),
)

object AuroraType {
    val displayXl = AuroraTypography.displayLarge
    val displayL = AuroraTypography.displayMedium
    val displayM = AuroraTypography.displaySmall
    val headlineL = AuroraTypography.headlineLarge
    val headlineM = AuroraTypography.headlineMedium
    val titleL = AuroraTypography.titleLarge
    val titleM = AuroraTypography.titleMedium
    val bodyL = AuroraTypography.bodyLarge
    val bodyM = AuroraTypography.bodyMedium
    val bodyS = AuroraTypography.bodySmall
    val labelL = AuroraTypography.labelLarge
    val labelM = AuroraTypography.labelMedium
    val overline = AuroraTypography.labelSmall
    val monoL = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em)
    val monoM = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.02.em)
    val monoS = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.06.em)
}

// ---------------------------------------------------------------------------
// Shape and dimensions
// ---------------------------------------------------------------------------

object AuroraShapes {
    val xs = RoundedCornerShape(10.dp)
    val sm = RoundedCornerShape(14.dp)
    val md = RoundedCornerShape(18.dp)
    val lg = RoundedCornerShape(22.dp)
    val xl = RoundedCornerShape(28.dp)
    val xxl = RoundedCornerShape(32.dp)
    val xxxl = RoundedCornerShape(36.dp)
    val sheet = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
    val full = RoundedCornerShape(percent = 50)
}

object AuroraDimens {
    val screenPadding = 20.dp
    val cardPadding = 20.dp
    val cardGap = 12.dp
    val sectionGap = 28.dp
    val touchTarget = 48.dp
    val buttonLarge = 52.dp
    val navBar = 68.dp
    val navInset = 16.dp
    val topBar = 56.dp
    val listRow = 64.dp
    val avatar = 40.dp
    val fab = 56.dp
    val shutter = 76.dp
}

// ---------------------------------------------------------------------------
// Composition locals
// ---------------------------------------------------------------------------

val LocalAuroraColors = staticCompositionLocalOf { AuroraLightColors }
val LocalAuroraMode = staticCompositionLocalOf { AuroraMode() }

object AuroraTheme {
    val colors: AuroraColors
        @Composable get() = LocalAuroraColors.current
    val mode: AuroraMode
        @Composable get() = LocalAuroraMode.current
    val type: AuroraType get() = AuroraType
    val shapes: AuroraShapes get() = AuroraShapes
    val dimens: AuroraDimens get() = AuroraDimens
}

// ---------------------------------------------------------------------------
// Theme
// ---------------------------------------------------------------------------

@Composable
fun AuroraTheme(
    mode: AuroraMode = AuroraMode.auto(
        night = isSystemInDarkTheme(),
        reduceTransparency = false,
    ),
    content: @Composable () -> Unit,
) {
    val colors = if (mode.night) AuroraNightColors else AuroraLightColors
    val scheme = if (mode.night) {
        darkColorScheme(
            primary = colors.iris600,
            onPrimary = Color(0xFF17151F),
            background = colors.canvasBase,
            onBackground = colors.ink900,
            surface = colors.solid,
            onSurface = colors.ink900,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.iris600,
            onPrimary = Color.White,
            background = colors.canvasBase,
            onBackground = colors.ink900,
            surface = colors.solid,
            onSurface = colors.ink900,
            error = colors.danger,
        )
    }

    CompositionLocalProvider(
        LocalAuroraColors provides colors,
        LocalAuroraMode provides mode,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AuroraTypography,
            shapes = androidx.compose.material3.Shapes(
                extraSmall = AuroraShapes.xs,
                small = AuroraShapes.sm,
                medium = AuroraShapes.md,
                large = AuroraShapes.xl,
                extraLarge = AuroraShapes.xxl,
            ),
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// Background
// ---------------------------------------------------------------------------

enum class AuroraBackgroundVariant { Orbit, Bloom, Plain }

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    variant: AuroraBackgroundVariant = AuroraBackgroundVariant.Orbit,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = AuroraTheme.colors
    val density = LocalDensity.current
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.canvasTop, c.canvasBase, c.canvasBottom)))) {
        if (variant != AuroraBackgroundVariant.Plain) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // Soft blooms
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(c.iris200.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(w * 0.82f, h * 0.16f),
                        radius = w * 0.62f,
                    ),
                    radius = w * 0.62f,
                    center = Offset(w * 0.82f, h * 0.16f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(c.tintRose.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(w * 0.12f, h * 0.86f),
                        radius = w * 0.55f,
                    ),
                    radius = w * 0.55f,
                    center = Offset(w * 0.12f, h * 0.86f),
                )
                if (variant == AuroraBackgroundVariant.Orbit) {
                    val stroke = with(density) { 1.dp.toPx() }
                    val path = Path().apply {
                        moveTo(-w * 0.1f, h * 0.28f)
                        cubicTo(w * 0.3f, h * 0.18f, w * 0.55f, h * 0.42f, w * 1.1f, h * 0.22f)
                    }
                    drawPath(path, c.canvasOrbit, style = Stroke(stroke))
                    val path2 = Path().apply {
                        moveTo(-w * 0.1f, h * 0.72f)
                        cubicTo(w * 0.35f, h * 0.60f, w * 0.62f, h * 0.88f, w * 1.1f, h * 0.66f)
                    }
                    drawPath(path2, c.canvasOrbit.copy(alpha = c.canvasOrbit.alpha * 0.6f), style = Stroke(stroke))
                }
            }
        }
        content()
    }
}

private fun AuroraColors.roseSoft(): Color = tintRose.copy(alpha = 0.45f)

// ---------------------------------------------------------------------------
// Glass surface
// ---------------------------------------------------------------------------

/**
 * A token-correct translucent card.
 *
 * True backdrop blur requires a backdrop-sampling implementation (RenderEffect
 * offscreen capture or a library such as Haze). Until that lands, Glass mode
 * uses a high-opacity fill; Solid mode is a first-class fallback and must look
 * equally intentional. Never animate blur radius.
 */
@Composable
fun AuroraGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = AuroraShapes.xl,
    elevated: Boolean = true,
    raised: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = AuroraTheme.colors
    val mode = AuroraTheme.mode.surface
    val fill = when {
        mode == AuroraSurfaceMode.Solid -> c.solidRaised
        raised -> c.glassRaised
        else -> c.glass
    }
    Box(
        modifier = modifier
            .then(if (elevated) Modifier.shadow(if (raised) 22.dp else 10.dp, shape) else Modifier)
            .clip(shape)
            .background(fill)
            .border(1.dp, if (mode == AuroraSurfaceMode.Solid) c.hairline else c.border, shape),
        content = content,
    )
}

// ---------------------------------------------------------------------------
// Utilities
// ---------------------------------------------------------------------------

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
