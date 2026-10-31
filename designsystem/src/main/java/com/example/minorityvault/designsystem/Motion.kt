package com.example.minorityvault.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

/**
 * Every so often a band of light sweeps across whatever this modifies. It only lights pixels that were
 * drawn, so it works on text as well as on buttons. Off when motion is reduced.
 */
fun Modifier.sheen(enabled: Boolean = true, initialDelayMillis: Long = 0L): Modifier = composed {
    val reducedMotion = VaultTheme.reducedMotion
    val progress = remember { Animatable(-1f) }
    LaunchedEffect(enabled, reducedMotion) {
        if (!enabled || reducedMotion) return@LaunchedEffect
        delay(initialDelayMillis)
        while (true) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(SHEEN_MILLIS, easing = LinearOutSlowInEasing))
            progress.snapTo(-1f)
            delay(SHEEN_PAUSE_MILLIS)
        }
    }
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val p = progress.value
            if (p >= 0f) {
                val band = size.width * SHEEN_BAND_RATIO
                val x = -band + (size.width + 2 * band) * p
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = SHEEN_ALPHA), Color.Transparent),
                        start = Offset(x - band, 0f),
                        end = Offset(x + band, size.height),
                    ),
                    blendMode = BlendMode.SrcAtop,
                )
            }
        }
}

/** Sinks slightly while pressed, like a heavy key. */
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !VaultTheme.reducedMotion) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press scale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Bumps up briefly whenever [value] grows, e.g. the pot. */
fun Modifier.pulseOnIncrease(value: Int): Modifier = composed {
    val reducedMotion = VaultTheme.reducedMotion
    val scale = remember { Animatable(1f) }
    var previous by remember { mutableIntStateOf(value) }
    LaunchedEffect(value) {
        val grew = value > previous
        previous = value
        if (grew && !reducedMotion) {
            scale.animateTo(PULSE_SCALE, tween(PULSE_UP_MILLIS))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * A number that counts to its new value instead of jumping. With [countFromZero] it also counts up the
 * first time it appears.
 */
@Composable
fun CountingText(
    value: Int,
    format: @Composable (Int) -> String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    countFromZero: Boolean = false,
) {
    val reducedMotion = VaultTheme.reducedMotion
    var target by remember { mutableIntStateOf(if (countFromZero && !reducedMotion) 0 else value) }
    LaunchedEffect(value) { target = value }
    val shown by animateIntAsState(
        targetValue = target,
        animationSpec = if (reducedMotion) snap() else tween(COUNT_MILLIS, easing = FastOutSlowInEasing),
        label = "counting number",
    )
    Text(format(shown), style = style, color = color, modifier = modifier)
}

private const val SHEEN_MILLIS = 900
private const val SHEEN_PAUSE_MILLIS = 4200L
private const val SHEEN_BAND_RATIO = 0.35f
private const val SHEEN_ALPHA = 0.45f
private const val PRESSED_SCALE = 0.95f
private const val PULSE_SCALE = 1.25f
private const val PULSE_UP_MILLIS = 120
private const val COUNT_MILLIS = 700
