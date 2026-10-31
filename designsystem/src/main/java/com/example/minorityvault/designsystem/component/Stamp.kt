package com.example.minorityvault.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.preview.ComponentPreviews

/** The red rubber stamp, for broken promises. Slams in once unless motion is reduced. */
@Composable
fun Stamp(text: String, modifier: Modifier = Modifier, rotation: Float = DEFAULT_ROTATION) {
    val color = VaultTheme.colors.stampRed
    val reducedMotion = VaultTheme.reducedMotion
    val scale = remember { Animatable(if (reducedMotion) 1f else SLAM_START_SCALE) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = color,
        modifier = modifier
            .scale(scale.value)
            .rotate(rotation)
            .border(2.5.dp, color, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = VaultSpacing.sm, vertical = VaultSpacing.xxs),
    )
}

private const val DEFAULT_ROTATION = -12f
private const val SLAM_START_SCALE = 1.8f

@ComponentPreviews
@Composable
private fun StampPreview() {
    VaultPreviewTheme {
        Stamp("Broken promise", Modifier.padding(VaultSpacing.xl))
    }
}
