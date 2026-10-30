package com.example.minorityvault.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.MinorityVaultTheme
import com.example.minorityvault.designsystem.R
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.preview.ComponentPreviews
import kotlin.math.cos
import kotlin.math.sin

/**
 * The reveal dial. The needle spins, then settles pointing at the smaller (winning) side: left for
 * [yesLabel], right for [noLabel], straight up on a tie. Reduced motion skips the spin.
 */
@Composable
fun VaultDial(yes: Int, no: Int, yesLabel: String, noLabel: String, modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    val reducedMotion = VaultTheme.reducedMotion
    val target = needleAngle(yes, no)
    val angle = remember { Animatable(if (reducedMotion) target else 0f) }
    LaunchedEffect(yes, no, reducedMotion) {
        if (reducedMotion) {
            angle.snapTo(target)
        } else {
            angle.snapTo(0f)
            angle.animateTo(SPIN_DEGREES + target, tween(SPIN_MILLIS, easing = EaseOutQuint))
        }
    }
    val description = stringResource(R.string.dial_description, yesLabel, yes, noLabel, no)
    Column(
        modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.widthIn(max = MAX_DIAL_SIZE).fillMaxWidth().aspectRatio(1f)) {
            val radius = size.minDimension / 2
            drawCircle(colors.panelRaised, radius)
            drawCircle(colors.steel, radius, style = Stroke(width = radius * RIM_RATIO))
            for (tick in 0 until TICK_COUNT) {
                val theta = Math.toRadians(tick * FULL_TURN.toDouble() / TICK_COUNT)
                val outer = radius * TICK_OUTER_RATIO
                val inner =
                    radius * if (tick % MAJOR_TICK_EVERY == 0) TICK_MAJOR_INNER_RATIO else TICK_MINOR_INNER_RATIO
                drawLine(
                    colors.steel,
                    Offset(center.x + (inner * sin(theta)).toFloat(), center.y - (inner * cos(theta)).toFloat()),
                    Offset(center.x + (outer * sin(theta)).toFloat(), center.y - (outer * cos(theta)).toFloat()),
                    strokeWidth = radius * TICK_WIDTH_RATIO,
                )
            }
            rotate(angle.value % FULL_TURN) {
                drawLine(
                    colors.brass,
                    center,
                    Offset(center.x, center.y - radius * NEEDLE_RATIO),
                    strokeWidth = radius * NEEDLE_WIDTH_RATIO,
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(colors.brass, radius * HUB_RATIO)
            drawCircle(colors.ink, radius * HUB_RATIO / 2)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = VaultSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DialLabel(yesLabel, yes)
            DialLabel(noLabel, no)
        }
    }
}

@Composable
private fun DialLabel(label: String, weight: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = VaultTheme.colors.onPanelMuted)
        Text("$weight", style = MaterialTheme.typography.displaySmall, color = VaultTheme.colors.onPanel)
    }
}

/** Degrees from straight up, negative to the left (YES), towards the side with less weight. */
internal fun needleAngle(yes: Int, no: Int): Float {
    val total = yes + no
    return if (total == 0) 0f else (yes - no).toFloat() / total * MAX_SWING_DEGREES
}

private val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val MAX_DIAL_SIZE = 280.dp
private const val MAX_SWING_DEGREES = 70f
private const val SPIN_DEGREES = 720f
private const val FULL_TURN = 360f
private const val SPIN_MILLIS = 2400
private const val TICK_COUNT = 60
private const val MAJOR_TICK_EVERY = 5
private const val RIM_RATIO = 0.06f
private const val TICK_OUTER_RATIO = 0.9f
private const val TICK_MAJOR_INNER_RATIO = 0.76f
private const val TICK_MINOR_INNER_RATIO = 0.83f
private const val TICK_WIDTH_RATIO = 0.015f
private const val NEEDLE_RATIO = 0.72f
private const val NEEDLE_WIDTH_RATIO = 0.05f
private const val HUB_RATIO = 0.1f

@ComponentPreviews
@Composable
private fun VaultDialPreview() {
    MinorityVaultTheme(reducedMotion = true) {
        VaultDial(yes = 2, no = 4, yesLabel = "Yes", noLabel = "No", modifier = Modifier.padding(VaultSpacing.lg))
    }
}
