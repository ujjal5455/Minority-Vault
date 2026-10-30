package com.example.minorityvault.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.MinorityVaultTheme
import com.example.minorityvault.designsystem.R
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.preview.ComponentPreviews

/** The Vault Keeper's speech: a keyhole avatar and a line of dry, theatrical narration. */
@Composable
fun KeeperBubble(line: String, modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(VaultSpacing.md), verticalAlignment = Alignment.Top) {
        KeyholeAvatar()
        Surface(
            shape = MaterialTheme.shapes.medium.copy(topStart = MaterialTheme.shapes.extraSmall.topStart),
            color = colors.panelRaised,
            contentColor = colors.onPanel,
        ) {
            Column(Modifier.padding(VaultSpacing.md)) {
                Text(
                    stringResource(R.string.keeper_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.brass,
                )
                Text(line, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun KeyholeAvatar(modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    val description = stringResource(R.string.keeper_avatar_description)
    Canvas(modifier.size(AVATAR_SIZE).semantics { contentDescription = description }) {
        val radius = size.minDimension / 2
        drawCircle(colors.brass, radius)
        drawCircle(colors.ink, radius * FACE_RATIO)
        val holeRadius = radius * HOLE_RATIO
        val holeCenter = Offset(center.x, center.y - radius * HOLE_RAISE_RATIO)
        drawCircle(colors.brass, holeRadius, holeCenter)
        val slotBottom = center.y + radius * SLOT_BOTTOM_RATIO
        val slot = Path().apply {
            moveTo(holeCenter.x - holeRadius * SLOT_TOP_HALF_WIDTH, holeCenter.y)
            lineTo(holeCenter.x + holeRadius * SLOT_TOP_HALF_WIDTH, holeCenter.y)
            lineTo(holeCenter.x + holeRadius * SLOT_BOTTOM_HALF_WIDTH, slotBottom)
            lineTo(holeCenter.x - holeRadius * SLOT_BOTTOM_HALF_WIDTH, slotBottom)
            close()
        }
        drawPath(slot, colors.brass)
    }
}

private val AVATAR_SIZE = 48.dp

// Keyhole proportions, relative to the avatar radius (or the hole radius for the slot widths).
private const val FACE_RATIO = 0.86f
private const val HOLE_RATIO = 0.26f
private const val HOLE_RAISE_RATIO = 0.16f
private const val SLOT_BOTTOM_RATIO = 0.5f
private const val SLOT_TOP_HALF_WIDTH = 0.55f
private const val SLOT_BOTTOM_HALF_WIDTH = 0.95f

@ComponentPreviews
@Composable
private fun KeeperBubblePreview() {
    MinorityVaultTheme {
        KeeperBubble(
            "Exactly two of you may win this round. The rest of you may console each other.",
            Modifier.padding(VaultSpacing.lg),
        )
    }
}
