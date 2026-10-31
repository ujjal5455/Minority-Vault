package com.example.minorityvault.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import com.example.minorityvault.designsystem.GlassSurface
import com.example.minorityvault.designsystem.GlassTint
import com.example.minorityvault.designsystem.MinTouchTarget
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.glassRim
import com.example.minorityvault.designsystem.pressScale
import com.example.minorityvault.designsystem.preview.ComponentPreviews
import com.example.minorityvault.designsystem.sheen

/** A glass button. It sinks when pressed; the primary one catches a sweep of light now and then. */
@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: VaultButtonStyle = VaultButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val colors = VaultTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val tint = when (style) {
        VaultButtonStyle.Primary -> GlassTint.Brass
        VaultButtonStyle.Secondary -> GlassTint.Panel
        VaultButtonStyle.Danger -> GlassTint.Danger
    }
    val content = when (style) {
        VaultButtonStyle.Primary -> colors.ink
        VaultButtonStyle.Secondary -> colors.brass
        VaultButtonStyle.Danger -> colors.paper
    }
    GlassSurface(
        modifier = modifier
            .defaultMinSize(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
            .pressScale(interactionSource)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .sheen(enabled = enabled && style == VaultButtonStyle.Primary),
        shape = MaterialTheme.shapes.small,
        tint = tint,
        contentColor = content,
        rim = if (style == VaultButtonStyle.Secondary) glassRim(colors.brass, BRASS_RIM_STRENGTH) else glassRim(),
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = VaultSpacing.lg, vertical = VaultSpacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}

enum class VaultButtonStyle {
    /** Brass glass, for the main action on a screen. */
    Primary,

    /** Smoked glass with a brass rim, for everything else. */
    Secondary,

    /** Stamp-red glass, for betrayal-flavoured actions only. */
    Danger,
}

private const val DISABLED_ALPHA = 0.4f
private const val BRASS_RIM_STRENGTH = 1.8f

@ComponentPreviews
@Composable
private fun VaultButtonPreview() {
    VaultPreviewTheme {
        Column(Modifier.padding(VaultSpacing.lg), verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
            VaultButton("Lock my vote", onClick = {}, modifier = Modifier.fillMaxWidth())
            VaultButton("Another round of turns", onClick = {}, style = VaultButtonStyle.Secondary)
            VaultButton("Flip to No", onClick = {}, style = VaultButtonStyle.Danger)
            VaultButton("Lock my vote", onClick = {}, enabled = false)
        }
    }
}
