package com.example.minorityvault.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import com.example.minorityvault.designsystem.VaultColors
import com.example.minorityvault.designsystem.VaultTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor

/**
 * A burst of brass coins thrown up from the centre that tumble and fall away. Plays once each time [trigger]
 * becomes true; nothing when motion is reduced. Draws outside its bounds and doesn't take touches, so lay
 * it over the content it celebrates.
 */
@Composable
fun CoinBurst(trigger: Boolean, modifier: Modifier = Modifier, coins: Int = DEFAULT_COINS) {
    val colors = VaultTheme.colors
    val reducedMotion = VaultTheme.reducedMotion
    val time = remember { Animatable(0f) }
    LaunchedEffect(trigger, reducedMotion) {
        if (trigger && !reducedMotion) {
            time.snapTo(0f)
            time.animateTo(1f, tween(BURST_MILLIS, easing = LinearEasing))
        }
    }
    Canvas(modifier) {
        val t = time.value
        if (t > 0f && t < 1f) {
            for (i in 0 until coins) drawCoin(i, coins, t, colors)
        }
    }
}

/** Each coin's path is fixed by its index, so the burst looks the same every time. */
private fun DrawScope.drawCoin(index: Int, count: Int, t: Float, colors: VaultColors) {
    val spread = (index.toFloat() / (count - 1) - SPREAD_CENTRE) * 2 // -1..1 across the burst
    val jitter = fraction(index * GOLDEN)
    val vx = spread * SPREAD_SPEED * size.width
    val vy = -(LAUNCH_SPEED + jitter * LAUNCH_JITTER) * size.height
    val gravity = GRAVITY * size.height
    val position = Offset(center.x + vx * t, center.y + vy * t + gravity * t * t)
    val radius = COIN_RADIUS * (1 + jitter * COIN_SIZE_JITTER) * density
    val tumble = abs(cos((t * TUMBLES + jitter) * 2 * Math.PI)).toFloat().coerceAtLeast(MIN_TUMBLE)
    val alpha = if (t > FADE_FROM) (1 - t) / (1 - FADE_FROM) else 1f
    scale(scaleX = tumble, scaleY = 1f, pivot = position) {
        drawCircle(colors.brass.copy(alpha = alpha), radius, position)
        drawCircle(
            colors.paper.copy(alpha = alpha * SHINE_ALPHA),
            radius * SHINE_RATIO,
            position - Offset(radius * SHINE_OFFSET, radius * SHINE_OFFSET),
        )
    }
}

private fun fraction(x: Float) = x - floor(x)

private const val DEFAULT_COINS = 18
private const val BURST_MILLIS = 1400
private const val GOLDEN = 0.618034f
private const val SPREAD_SPEED = 0.55f
private const val LAUNCH_SPEED = 1.1f
private const val LAUNCH_JITTER = 0.6f
private const val GRAVITY = 2.4f
private const val COIN_RADIUS = 7f
private const val COIN_SIZE_JITTER = 0.5f
private const val TUMBLES = 2.5f
private const val MIN_TUMBLE = 0.15f
private const val FADE_FROM = 0.7f
private const val SHINE_ALPHA = 0.7f
private const val SHINE_RATIO = 0.4f
private const val SHINE_OFFSET = 0.33f
private const val SPREAD_CENTRE = 0.5f
