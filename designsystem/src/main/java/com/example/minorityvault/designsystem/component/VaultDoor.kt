package com.example.minorityvault.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.VaultColors
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.preview.ComponentPreviews
import kotlin.math.cos
import kotlin.math.sin

/**
 * A round vault door with locking bolts and a spoked brass wheel. [wheelDegrees] turns the wheel; callers
 * swing the whole door open with a graphicsLayer. Decorative.
 */
@Composable
fun VaultDoor(wheelDegrees: Float, modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    Canvas(modifier.widthIn(max = MAX_DOOR_SIZE).fillMaxWidth().aspectRatio(1f)) {
        val radius = size.minDimension / 2
        drawDoorBody(radius, colors)
        drawBolts(radius, colors)
        rotate(wheelDegrees) { drawWheel(radius, colors) }
    }
}

private fun DrawScope.drawDoorBody(radius: Float, colors: VaultColors) {
    // Outer frame: brushed steel, lit from the top left.
    drawCircle(
        Brush.linearGradient(
            listOf(colors.paper, colors.steel, colors.ink),
            Offset.Zero,
            Offset(size.width, size.height),
        ),
        radius,
    )
    // The door face: smoked glass over the frame.
    drawCircle(
        Brush.radialGradient(
            listOf(colors.panelRaised, colors.panel, colors.ink),
            center = center - Offset(radius * LIGHT_OFFSET, radius * LIGHT_OFFSET),
            radius = radius * 2,
        ),
        radius * FACE_RATIO,
    )
    drawCircle(
        Color.White.copy(alpha = GROOVE_ALPHA),
        radius * GROOVE_RATIO,
        style = Stroke(
            width =
            radius * GROOVE_WIDTH,
        ),
    )
}

private fun DrawScope.drawBolts(radius: Float, colors: VaultColors) {
    for (i in 0 until BOLT_COUNT) {
        val theta = 2 * Math.PI * i / BOLT_COUNT
        val at = Offset(
            center.x + (radius * BOLT_RING * cos(theta)).toFloat(),
            center.y + (radius * BOLT_RING * sin(theta)).toFloat(),
        )
        drawCircle(colors.steel, radius * BOLT_SIZE, at)
        drawCircle(Color.White.copy(alpha = BOLT_GLINT_ALPHA), radius * BOLT_SIZE / 2, at - Offset(2f, 2f))
    }
}

private fun DrawScope.drawWheel(radius: Float, colors: VaultColors) {
    val wheel = radius * WHEEL_RATIO
    for (i in 0 until SPOKE_COUNT) {
        val theta = 2 * Math.PI * i / SPOKE_COUNT
        val tip = Offset(center.x + (wheel * cos(theta)).toFloat(), center.y + (wheel * sin(theta)).toFloat())
        drawLine(colors.brass, center, tip, strokeWidth = radius * SPOKE_WIDTH, cap = StrokeCap.Round)
        drawCircle(colors.brass, radius * SPOKE_KNOB, tip)
    }
    drawCircle(colors.brass, wheel, style = Stroke(width = radius * RIM_WIDTH))
    drawCircle(colors.brass, radius * HUB_RATIO)
    drawCircle(colors.ink, radius * HUB_RATIO / 2)
}

private val MAX_DOOR_SIZE = 260.dp
private const val LIGHT_OFFSET = 0.5f
private const val FACE_RATIO = 0.88f
private const val GROOVE_RATIO = 0.7f
private const val GROOVE_WIDTH = 0.015f
private const val GROOVE_ALPHA = 0.12f
private const val BOLT_COUNT = 10
private const val BOLT_RING = 0.8f
private const val BOLT_SIZE = 0.045f
private const val BOLT_GLINT_ALPHA = 0.5f
private const val WHEEL_RATIO = 0.42f
private const val SPOKE_COUNT = 3
private const val SPOKE_WIDTH = 0.05f
private const val SPOKE_KNOB = 0.06f
private const val RIM_WIDTH = 0.04f
private const val HUB_RATIO = 0.11f

@ComponentPreviews
@Composable
private fun VaultDoorPreview() {
    VaultPreviewTheme {
        VaultDoor(wheelDegrees = 20f, modifier = Modifier.padding(VaultSpacing.xl))
    }
}
