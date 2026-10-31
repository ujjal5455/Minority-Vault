package com.example.minorityvault.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/** The light behind the glass. Null outside a [VaultBackdrop] (e.g. some previews): glass falls back to a tint. */
internal val LocalVaultBackdrop = staticCompositionLocalOf<HazeState?> { null }

/**
 * Ink green with slow-drifting brass and teal light and a few twinkling sparks. Every [GlassSurface] inside
 * blurs this, which is what makes the panels read as frosted glass. Drifts at a low frame rate to save
 * battery; holds still when motion is reduced.
 */
@Composable
fun VaultBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val state = rememberHazeState()
    val colors = VaultTheme.colors
    val reducedMotion = VaultTheme.reducedMotion
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) {
            seconds = withFrameMillis { (it - start) / MILLIS_PER_SECOND }
            delay(BACKDROP_FRAME_MILLIS)
        }
    }
    Box(modifier.fillMaxSize().background(colors.ink)) {
        Box(Modifier.matchParentSize().hazeSource(state).drawBehind { drawGlow(seconds, colors) })
        CompositionLocalProvider(LocalVaultBackdrop provides state) { content() }
    }
}

private fun DrawScope.drawGlow(seconds: Float, colors: VaultColors) {
    val drift = { period: Float, phase: Float -> sin(2 * PI * (seconds / period + phase)).toFloat() }
    val radius = size.maxDimension * GLOW_RADIUS_RATIO
    val brassCenter = Offset(
        size.width * (BRASS_X + DRIFT_RATIO * drift(BRASS_PERIOD, 0f)),
        size.height * (BRASS_Y + DRIFT_RATIO * drift(BRASS_PERIOD, QUARTER)),
    )
    val tealCenter = Offset(
        size.width * (TEAL_X + DRIFT_RATIO * drift(TEAL_PERIOD, HALF)),
        size.height * (TEAL_Y + DRIFT_RATIO * drift(TEAL_PERIOD, QUARTER)),
    )
    drawCircle(
        Brush.radialGradient(
            listOf(colors.brass.copy(alpha = BRASS_GLOW_ALPHA), Color.Transparent),
            brassCenter,
            radius,
        ),
        radius,
        brassCenter,
    )
    drawCircle(
        Brush.radialGradient(listOf(TealGlow.copy(alpha = TEAL_GLOW_ALPHA), Color.Transparent), tealCenter, radius),
        radius,
        tealCenter,
    )
    SPARKS.forEachIndexed { i, (x, y) ->
        val twinkle = (1 + drift(SPARK_PERIOD + i, i * SPARK_PHASE_STEP)) / 2
        drawCircle(
            colors.paper.copy(alpha = SPARK_MIN_ALPHA + SPARK_ALPHA_RANGE * twinkle),
            radius = SPARK_RADIUS.toPx() * (1 + twinkle),
            center = Offset(size.width * x, size.height * y),
        )
    }
}

/** How a piece of glass is tinted. Darker tints keep light text readable; paper keeps dark text readable. */
enum class GlassTint(internal val color: (VaultColors) -> Color, internal val alpha: Float) {
    /** The default panel: smoked green glass. */
    Panel({ it.panel }, alpha = 0.55f),

    /** A little lighter, for raised things such as your own chat bubbles. */
    Raised({ it.panelRaised }, alpha = 0.62f),

    /** Frosted paper, for anything official (questions, contracts). Use dark text on it. */
    Paper({ it.paper }, alpha = 0.86f),

    /** Brass, for primary actions and money. */
    Brass({ it.brass }, alpha = 0.96f),

    /** Stamp red, for betrayal and errors only. */
    Danger({ it.stampRed }, alpha = 0.85f),
}

/**
 * A pane of frosted glass: blurs the [VaultBackdrop] behind it, tints it, and catches the light along its
 * rim. On Android 11 and older, where blur isn't available, the tint is simply more opaque.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = VaultShapes.medium,
    tint: GlassTint = GlassTint.Panel,
    contentColor: Color = if (tint == GlassTint.Paper) VaultTheme.colors.paperInk else VaultTheme.colors.onPanel,
    rim: Brush = glassRim(),
    blurRadius: Dp = GLASS_BLUR,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = VaultTheme.colors
    val backdrop = LocalVaultBackdrop.current
    val tintColor = tint.color(colors).copy(alpha = tint.alpha)
    val style = remember(tintColor, blurRadius) {
        HazeBlurStyle {
            blurRadius(blurRadius)
            noiseFactor(GLASS_NOISE)
            colorEffects(listOf(HazeColorEffect.tint(tintColor)))
            fallbackColorEffect(HazeColorEffect.tint(tintColor.copy(alpha = FALLBACK_ALPHA)))
        }
    }
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Box(
        modifier
            .clip(shape)
            .then(
                if (backdrop != null) {
                    Modifier.hazeBlur(HazeInput.Sources(backdrop), style)
                } else {
                    Modifier.background(tintColor.copy(alpha = FALLBACK_ALPHA))
                },
            )
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = SHEEN_TOP_ALPHA), Color.Transparent)))
            .border(BorderStroke(RIM_WIDTH, rim), shape)
            .then(clickModifier),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

/** Light catching the top-left edge of a pane, fading round to a faint glint bottom-right. */
fun glassRim(color: Color = Color.White, strength: Float = 1f): Brush = Brush.linearGradient(
    0f to color.copy(alpha = RIM_BRIGHT * strength),
    RIM_DIM_STOP to color.copy(alpha = RIM_DIM * strength),
    1f to color.copy(alpha = RIM_GLINT * strength),
)

/** For previews: the theme plus the backdrop, so glass looks the way it does in the app. */
@Composable
fun VaultPreviewTheme(reducedMotion: Boolean = true, content: @Composable () -> Unit) {
    MinorityVaultTheme(reducedMotion = reducedMotion) {
        VaultBackdrop { content() }
    }
}

private val TealGlow = Color(0xFF3FA796)
private val SPARKS = listOf(0.12f to 0.18f, 0.78f to 0.1f, 0.62f to 0.42f, 0.2f to 0.66f, 0.86f to 0.74f, 0.4f to 0.9f)
private val GLASS_BLUR = 24.dp
private val RIM_WIDTH = 1.dp
private val SPARK_RADIUS = 1.dp
private const val GLASS_NOISE = 0.06f
private const val FALLBACK_ALPHA = 0.94f
private const val SHEEN_TOP_ALPHA = 0.07f
private const val RIM_BRIGHT = 0.42f
private const val RIM_DIM = 0.08f
private const val RIM_GLINT = 0.2f
private const val RIM_DIM_STOP = 0.45f
private const val MILLIS_PER_SECOND = 1000f
private const val BACKDROP_FRAME_MILLIS = 66L
private const val GLOW_RADIUS_RATIO = 0.55f
private const val DRIFT_RATIO = 0.06f
private const val BRASS_X = 0.18f
private const val BRASS_Y = 0.22f
private const val TEAL_X = 0.85f
private const val TEAL_Y = 0.78f
private const val BRASS_PERIOD = 23f
private const val TEAL_PERIOD = 31f
private const val SPARK_PERIOD = 4f
private const val SPARK_PHASE_STEP = 0.37f
private const val QUARTER = 0.25f
private const val HALF = 0.5f
private const val BRASS_GLOW_ALPHA = 0.32f
private const val TEAL_GLOW_ALPHA = 0.28f
private const val SPARK_MIN_ALPHA = 0.06f
private const val SPARK_ALPHA_RANGE = 0.3f
