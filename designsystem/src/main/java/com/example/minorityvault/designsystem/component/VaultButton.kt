package com.example.minorityvault.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.MinTouchTarget
import com.example.minorityvault.designsystem.MinorityVaultTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.preview.ComponentPreviews

@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: VaultButtonStyle = VaultButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val colors = VaultTheme.colors
    val container = when (style) {
        VaultButtonStyle.Primary -> colors.brass
        VaultButtonStyle.Secondary -> Color.Transparent
        VaultButtonStyle.Danger -> colors.stampRed
    }
    val content = when (style) {
        VaultButtonStyle.Primary -> colors.ink
        VaultButtonStyle.Secondary -> colors.brass
        VaultButtonStyle.Danger -> colors.paper
    }
    val border = if (style == VaultButtonStyle.Secondary) BorderStroke(1.5.dp, colors.brass) else null
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minWidth = MinTouchTarget, minHeight = MinTouchTarget),
        shape = MaterialTheme.shapes.small,
        color = if (enabled) container else container.copy(alpha = DISABLED_ALPHA),
        contentColor = if (enabled) content else content.copy(alpha = DISABLED_ALPHA),
        border = border,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = VaultSpacing.lg, vertical = VaultSpacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}

enum class VaultButtonStyle {
    /** Brass, for the main action on a screen. */
    Primary,

    /** Brass outline, for everything else. */
    Secondary,

    /** Stamp red, for betrayal-flavoured actions only. */
    Danger,
}

private const val DISABLED_ALPHA = 0.4f

@ComponentPreviews
@Composable
private fun VaultButtonPreview() {
    MinorityVaultTheme {
        Column(Modifier.padding(VaultSpacing.lg), verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
            VaultButton("Lock my vote", onClick = {}, modifier = Modifier.fillMaxWidth())
            VaultButton("Another round of turns", onClick = {}, style = VaultButtonStyle.Secondary)
            VaultButton("Break the deal", onClick = {}, style = VaultButtonStyle.Danger)
            VaultButton("Lock my vote", onClick = {}, enabled = false)
        }
    }
}
