package com.example.minorityvault.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.MinorityVaultTheme
import com.example.minorityvault.designsystem.R
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.preview.ComponentPreviews

/**
 * A player at the table. [hasLocked] draws the lock dot during VOTE (null hides it). Broken promises show as
 * a red mark; it's public by design.
 */
@Composable
fun PlayerChip(
    name: String,
    balance: Int,
    modifier: Modifier = Modifier,
    brokenPromises: Int = 0,
    isDebtor: Boolean = false,
    hasLocked: Boolean? = null,
    highlighted: Boolean = false,
) {
    val colors = VaultTheme.colors
    val description = playerDescription(name, balance, brokenPromises, isDebtor, hasLocked)
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = MaterialTheme.shapes.medium,
        color = colors.panel,
        contentColor = colors.onPanel,
        border = BorderStroke(1.dp, if (highlighted) colors.brass else colors.panelRaised),
    ) {
        Row(
            Modifier.padding(VaultSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Initials(name, hasLocked)
            Column(Modifier.weight(1f, fill = false)) {
                Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.coins, balance, balance),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.brass,
                )
                if (isDebtor || brokenPromises > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.xs)) {
                        if (isDebtor) Badge(stringResource(R.string.debtor_badge), colors.stampRed)
                        if (brokenPromises > 0) Badge("✕$brokenPromises", colors.stampRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun Initials(name: String, hasLocked: Boolean?) {
    val colors = VaultTheme.colors
    Box {
        Box(
            Modifier.size(AVATAR_SIZE).background(colors.steel, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(initialsOf(name), style = MaterialTheme.typography.titleMedium, color = colors.ink)
        }
        if (hasLocked != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(LOCK_DOT_SIZE)
                    .background(if (hasLocked) colors.brass else colors.ink, CircleShape)
                    .border(2.dp, if (hasLocked) colors.panel else colors.steel, CircleShape),
            )
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .border(1.dp, color, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = VaultSpacing.xs),
    )
}

@Composable
private fun playerDescription(
    name: String,
    balance: Int,
    brokenPromises: Int,
    isDebtor: Boolean,
    hasLocked: Boolean?,
): String = listOfNotNull(
    name,
    pluralStringResource(R.plurals.coins, balance, balance),
    if (brokenPromises > 0) pluralStringResource(R.plurals.broken_promises, brokenPromises, brokenPromises) else null,
    if (isDebtor) stringResource(R.string.debtor_badge) else null,
    when (hasLocked) {
        true -> stringResource(R.string.player_locked)
        false -> stringResource(R.string.player_not_locked)
        null -> null
    },
).joinToString(", ")

internal fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }

private val AVATAR_SIZE = 40.dp
private val LOCK_DOT_SIZE = 14.dp

@ComponentPreviews
@Composable
private fun PlayerChipPreview() {
    MinorityVaultTheme {
        Column(Modifier.padding(VaultSpacing.lg), verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
            PlayerChip("Asha Rao", balance = 115, highlighted = true)
            PlayerChip("Ben", balance = 62, brokenPromises = 2, hasLocked = true)
            PlayerChip("Chandrika Venkataraman", balance = 0, isDebtor = true, hasLocked = false)
        }
    }
}
