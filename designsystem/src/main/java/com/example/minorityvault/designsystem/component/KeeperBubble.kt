package com.example.minorityvault.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.GlassSurface
import com.example.minorityvault.designsystem.GlassTint
import com.example.minorityvault.designsystem.R
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.preview.ComponentPreviews
import kotlinx.coroutines.delay

/**
 * The Vault Keeper speaks: the line types itself out while the keyhole glows. The full line is laid out
 * from the start (the unspoken part is invisible), so nothing jumps, and screen readers get it all at once.
 */
@Composable
fun KeeperBubble(line: String, modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    val reducedMotion = VaultTheme.reducedMotion
    var shown by remember(line) { mutableIntStateOf(if (reducedMotion) line.length else 0) }
    LaunchedEffect(line, reducedMotion) {
        if (reducedMotion) {
            shown = line.length
        } else {
            while (shown < line.length) {
                delay(CHAR_MILLIS)
                shown++
            }
        }
    }
    val speaking = shown < line.length
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(VaultSpacing.md), verticalAlignment = Alignment.Top) {
        KeyholeAvatar(glowing = speaking)
        GlassSurface(
            shape = MaterialTheme.shapes.medium.copy(topStart = MaterialTheme.shapes.extraSmall.topStart),
            tint = GlassTint.Raised,
        ) {
            Column(Modifier.padding(VaultSpacing.md)) {
                Text(
                    stringResource(R.string.keeper_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.brass,
                )
                Text(
                    buildAnnotatedString {
                        append(line.take(shown))
                        withStyle(SpanStyle(color = Color.Transparent)) { append(line.drop(shown)) }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(line) },
                )
            }
        }
    }
}

@Composable
private fun KeyholeAvatar(glowing: Boolean, modifier: Modifier = Modifier) {
    val colors = VaultTheme.colors
    val description = stringResource(R.string.keeper_avatar_description)
    val glow by animateFloatAsState(if (glowing) GLOW_ALPHA else 0f, tween(GLOW_FADE_MILLIS), label = "keyhole glow")
    Canvas(modifier.size(AVATAR_SIZE).semantics { contentDescription = description }) {
        val radius = size.minDimension / 2
        if (glow > 0f) {
            drawCircle(
                Brush.radialGradient(
                    listOf(colors.brass.copy(alpha = glow), Color.Transparent),
                    center,
                    radius * GLOW_REACH,
                ),
                radius * GLOW_REACH,
            )
        }
        drawCircle(colors.brass, radius)
        drawCircle(colors.ink, radius * FACE_RATIO)
        val holeRadius = radius * HOLE_RATIO
        val holeCenter = Offset(center.x, center.y - radius * HOLE_RAISE_RATIO)
        val light = if (glowing) colors.paper else colors.brass
        drawCircle(light, holeRadius, holeCenter)
        val slotBottom = center.y + radius * SLOT_BOTTOM_RATIO
        val slot = Path().apply {
            moveTo(holeCenter.x - holeRadius * SLOT_TOP_HALF_WIDTH, holeCenter.y)
            lineTo(holeCenter.x + holeRadius * SLOT_TOP_HALF_WIDTH, holeCenter.y)
            lineTo(holeCenter.x + holeRadius * SLOT_BOTTOM_HALF_WIDTH, slotBottom)
            lineTo(holeCenter.x - holeRadius * SLOT_BOTTOM_HALF_WIDTH, slotBottom)
            close()
        }
        drawPath(slot, light)
    }
}

private val AVATAR_SIZE = 48.dp
private const val CHAR_MILLIS = 22L
private const val GLOW_ALPHA = 0.55f
private const val GLOW_FADE_MILLIS = 600
private const val GLOW_REACH = 1.7f

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
    VaultPreviewTheme {
        KeeperBubble(
            "Exactly two of you may win this round. The rest of you may console each other.",
            Modifier.padding(VaultSpacing.lg),
        )
    }
}
