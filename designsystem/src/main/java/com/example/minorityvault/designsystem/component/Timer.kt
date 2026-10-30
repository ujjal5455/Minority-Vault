package com.example.minorityvault.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.MinorityVaultTheme
import com.example.minorityvault.designsystem.R
import com.example.minorityvault.designsystem.URGENT_SECONDS
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.preview.ComponentPreviews

/** "2:05". Turns stamp red and pulses for the last [URGENT_SECONDS] seconds. */
@Composable
fun TimerText(secondsLeft: Int, modifier: Modifier = Modifier) {
    val urgent = secondsLeft <= URGENT_SECONDS
    val colors = VaultTheme.colors
    val formatted = formatSeconds(secondsLeft)
    val description = stringResource(R.string.timer_description, formatted)
    Text(
        text = formatted,
        style = MaterialTheme.typography.headlineMedium,
        color = if (urgent) colors.stampRed else colors.onPanel,
        modifier = modifier
            .scale(if (urgent) pulseScale() else 1f)
            .semantics { contentDescription = description },
    )
}

/** A draining bar: [fraction] is the share of the phase left, from 1 down to 0. */
@Composable
fun TimerBar(fraction: Float, secondsLeft: Int, modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .clip(RoundedCornerShape(BAR_HEIGHT))
            .background(colors.steel.copy(alpha = TRACK_ALPHA))
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f) },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .background(if (secondsLeft <= URGENT_SECONDS) colors.stampRed else colors.brass),
        )
    }
}

@Composable
private fun pulseScale(): Float {
    if (VaultTheme.reducedMotion) return 1f
    val transition = rememberInfiniteTransition(label = "timer pulse")
    val scale = transition.animateFloat(
        initialValue = 1f,
        targetValue = PULSE_SCALE,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "timer pulse scale",
    )
    return scale.value
}

private fun formatSeconds(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%d:%02d".format(safe / SECONDS_PER_MINUTE, safe % SECONDS_PER_MINUTE)
}

private val BAR_HEIGHT = 6.dp
private const val TRACK_ALPHA = 0.25f
private const val PULSE_SCALE = 1.08f
private const val PULSE_MILLIS = 500
private const val SECONDS_PER_MINUTE = 60

@ComponentPreviews
@Composable
private fun TimerPreview() {
    MinorityVaultTheme {
        Column(Modifier.padding(VaultSpacing.lg), verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
            TimerText(secondsLeft = 125)
            TimerBar(fraction = 0.8f, secondsLeft = 125)
            TimerText(secondsLeft = 7)
            TimerBar(fraction = 0.05f, secondsLeft = 7)
        }
    }
}
