package com.example.minorityvault.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.glassRim
import com.example.minorityvault.designsystem.preview.ComponentPreviews

/**
 * A brass bolt that slides home into its keeper when a vote is locked, with a heavy little bounce.
 * Decorative: the locked state is also stated in text for screen readers.
 */
@Composable
fun VaultBolt(locked: Boolean, modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    val travel by animateFloatAsState(
        targetValue = if (locked) 1f else 0f,
        animationSpec = if (VaultTheme.reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = BOLT_DAMPING,
                stiffness = BOLT_STIFFNESS,
            )
        },
        label = "bolt travel",
    )
    Canvas(modifier.fillMaxWidth().height(BOLT_HEIGHT)) {
        val radius = CornerRadius(size.height / 2)
        // The glass track.
        drawRoundRect(Color.White.copy(alpha = TRACK_ALPHA), cornerRadius = radius)
        drawRoundRect(glassRim(), cornerRadius = radius, style = Stroke(width = 1.dp.toPx()))
        // The keeper the bolt slides into, on the right.
        val keeperWidth = size.width * KEEPER_RATIO
        drawRoundRect(
            colors.steel,
            topLeft = Offset(size.width - keeperWidth, 0f),
            size = Size(keeperWidth, size.height),
            cornerRadius = radius,
        )
        // The bolt itself: brass, lit from above, its head sliding from the left into the keeper.
        val boltWidth = size.width * BOLT_RATIO
        val inset = size.height * INSET_RATIO
        val home = size.width - boltWidth - keeperWidth / 2
        val left = inset + (home - inset) * travel
        drawRoundRect(
            Brush.verticalGradient(
                listOf(
                    colors.paper.copy(alpha = HIGHLIGHT_ALPHA),
                    colors.brass,
                    colors.brass.copy(alpha = SHADE_ALPHA),
                ),
            ),
            topLeft = Offset(left, inset),
            size = Size(boltWidth, size.height - 2 * inset),
            cornerRadius = CornerRadius((size.height - 2 * inset) / 2),
        )
    }
}

private val BOLT_HEIGHT = 22.dp
private const val BOLT_STIFFNESS = 500f
private const val BOLT_DAMPING = 0.5f
private const val TRACK_ALPHA = 0.06f
private const val KEEPER_RATIO = 0.12f
private const val BOLT_RATIO = 0.42f
private const val INSET_RATIO = 0.18f
private const val HIGHLIGHT_ALPHA = 0.9f
private const val SHADE_ALPHA = 0.8f

@ComponentPreviews
@Composable
private fun VaultBoltPreview() {
    VaultPreviewTheme {
        Column(Modifier.padding(VaultSpacing.lg), verticalArrangement = Arrangement.spacedBy(VaultSpacing.md)) {
            VaultBolt(locked = false)
            VaultBolt(locked = true)
        }
    }
}
