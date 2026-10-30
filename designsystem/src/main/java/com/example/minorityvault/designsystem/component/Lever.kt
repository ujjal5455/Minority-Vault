package com.example.minorityvault.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.MinorityVaultTheme
import com.example.minorityvault.designsystem.R
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.preview.ComponentPreviews

/**
 * The vote lever. Two of them side by side make the YES/NO choice; pulling one is a selection, not a lock.
 * Behaves like a radio button for accessibility.
 */
@Composable
fun Lever(label: String, pulled: Boolean, onPull: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = VaultTheme.colors
    val position by animateFloatAsState(
        targetValue = if (pulled) 1f else 0f,
        animationSpec = if (VaultTheme.reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
            )
        },
        label = "lever handle",
    )
    val description = stringResource(R.string.lever_description, label)
    val state = stringResource(if (pulled) R.string.lever_pulled else R.string.lever_not_pulled)
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .selectable(selected = pulled, enabled = enabled, role = Role.RadioButton, onClick = onPull)
            .semantics {
                contentDescription = description
                stateDescription = state
            }
            .padding(VaultSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
    ) {
        Canvas(Modifier.size(width = LEVER_WIDTH, height = LEVER_HEIGHT)) {
            val slotWidth = size.width * SLOT_WIDTH_RATIO
            drawRoundRect(
                color = colors.panelRaised,
                topLeft = Offset((size.width - slotWidth) / 2, 0f),
                size = Size(slotWidth, size.height),
                cornerRadius = CornerRadius(slotWidth / 2),
            )
            val pivot = Offset(size.width / 2, size.height / 2)
            val travel = size.height * TRAVEL_RATIO
            val knob = Offset(size.width / 2, pivot.y - travel + 2 * travel * position)
            drawLine(colors.steel, pivot, knob, strokeWidth = size.width * ARM_WIDTH_RATIO, cap = StrokeCap.Round)
            drawCircle(colors.steel, radius = size.width * PIVOT_RATIO, center = pivot)
            drawCircle(
                if (pulled) colors.brass else colors.onPanelMuted,
                radius = size.width * KNOB_RATIO,
                center = knob,
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.headlineMedium,
            color = if (pulled) colors.brass else colors.onPanel,
        )
    }
}

private val LEVER_WIDTH = 72.dp
private val LEVER_HEIGHT = 168.dp
private const val SLOT_WIDTH_RATIO = 0.28f
private const val TRAVEL_RATIO = 0.36f
private const val ARM_WIDTH_RATIO = 0.12f
private const val PIVOT_RATIO = 0.14f
private const val KNOB_RATIO = 0.3f
private const val DISABLED_ALPHA = 0.4f

@ComponentPreviews
@Composable
private fun LeverPreview() {
    MinorityVaultTheme {
        var choice by remember { mutableStateOf<String?>("NO") }
        Row(Modifier.padding(VaultSpacing.lg), horizontalArrangement = Arrangement.spacedBy(VaultSpacing.xl)) {
            Lever("Yes", pulled = choice == "YES", onPull = { choice = "YES" })
            Lever("No", pulled = choice == "NO", onPull = { choice = "NO" })
            Lever("Locked", pulled = true, onPull = {}, enabled = false)
        }
    }
}
