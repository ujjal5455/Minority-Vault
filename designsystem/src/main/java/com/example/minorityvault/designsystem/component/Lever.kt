package com.example.minorityvault.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.R
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.glassRim
import com.example.minorityvault.designsystem.pressScale
import com.example.minorityvault.designsystem.preview.ComponentPreviews

/**
 * The vote lever. Two of them side by side make the YES/NO choice; pulling one is a selection, not a lock.
 * Behaves like a radio button for accessibility.
 */
@Composable
fun Lever(label: String, pulled: Boolean, onPull: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = VaultTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    // A low-damping spring: the handle overshoots and settles, like a heavy lever.
    val position by animateFloatAsState(
        targetValue = if (pulled) 1f else 0f,
        animationSpec = if (VaultTheme.reducedMotion) {
            snap()
        } else {
            spring(dampingRatio = LEVER_DAMPING, stiffness = Spring.StiffnessMediumLow)
        },
        label = "lever handle",
    )
    val description = stringResource(R.string.lever_description, label)
    val state = stringResource(if (pulled) R.string.lever_pulled else R.string.lever_not_pulled)
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressScale(interactionSource)
            .selectable(
                selected = pulled,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onPull,
            )
            .semantics {
                contentDescription = description
                stateDescription = state
            }
            .padding(VaultSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
    ) {
        Canvas(Modifier.size(width = LEVER_WIDTH, height = LEVER_HEIGHT)) {
            drawLever(position, metal = if (pulled) colors.brass else colors.steel, arm = colors.steel)
        }
        Text(
            label,
            style = MaterialTheme.typography.headlineMedium,
            color = if (pulled) colors.brass else colors.onPanel,
        )
    }
}

/** A glass slot with a rim of light, a steel arm, and a metal knob lit from the top left. */
private fun DrawScope.drawLever(position: Float, metal: Color, arm: Color) {
    val slotWidth = size.width * SLOT_WIDTH_RATIO
    val slotTopLeft = Offset((size.width - slotWidth) / 2, 0f)
    val slotSize = Size(slotWidth, size.height)
    drawRoundRect(Color.White.copy(alpha = SLOT_FILL_ALPHA), slotTopLeft, slotSize, CornerRadius(slotWidth / 2))
    drawRoundRect(
        glassRim(),
        slotTopLeft,
        slotSize,
        CornerRadius(slotWidth / 2),
        style = Stroke(width = SLOT_RIM_WIDTH.toPx()),
    )
    val pivot = Offset(size.width / 2, size.height / 2)
    val travel = size.height * TRAVEL_RATIO
    val knob = Offset(size.width / 2, pivot.y - travel + 2 * travel * position)
    drawLine(arm, pivot, knob, strokeWidth = size.width * ARM_WIDTH_RATIO, cap = StrokeCap.Round)
    drawCircle(arm, radius = size.width * PIVOT_RATIO, center = pivot)
    val knobRadius = size.width * KNOB_RATIO
    drawCircle(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = KNOB_HIGHLIGHT_ALPHA), metal, metal.copy(alpha = KNOB_SHADE_ALPHA)),
            center = knob - Offset(knobRadius * KNOB_LIGHT_OFFSET, knobRadius * KNOB_LIGHT_OFFSET),
            radius = knobRadius * 2,
        ),
        radius = knobRadius,
        center = knob,
    )
}

private val LEVER_WIDTH = 72.dp
private val LEVER_HEIGHT = 168.dp
private const val SLOT_WIDTH_RATIO = 0.28f
private const val TRAVEL_RATIO = 0.36f
private const val ARM_WIDTH_RATIO = 0.12f
private const val PIVOT_RATIO = 0.14f
private const val KNOB_RATIO = 0.3f
private const val DISABLED_ALPHA = 0.4f
private const val LEVER_DAMPING = 0.42f
private const val SLOT_FILL_ALPHA = 0.08f
private val SLOT_RIM_WIDTH = 1.dp
private const val KNOB_HIGHLIGHT_ALPHA = 0.85f
private const val KNOB_SHADE_ALPHA = 0.75f
private const val KNOB_LIGHT_OFFSET = 0.4f

@ComponentPreviews
@Composable
private fun LeverPreview() {
    VaultPreviewTheme {
        var choice by remember { mutableStateOf<String?>("NO") }
        Row(Modifier.padding(VaultSpacing.lg), horizontalArrangement = Arrangement.spacedBy(VaultSpacing.xl)) {
            Lever("Yes", pulled = choice == "YES", onPull = { choice = "YES" })
            Lever("No", pulled = choice == "NO", onPull = { choice = "NO" })
            Lever("Locked", pulled = true, onPull = {}, enabled = false)
        }
    }
}
