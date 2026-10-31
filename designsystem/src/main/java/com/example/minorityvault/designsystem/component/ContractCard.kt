package com.example.minorityvault.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.minorityvault.designsystem.R
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.preview.ComponentPreviews

/**
 * A contract on paper. [terms] is the plain-words version of the structured terms. Pass [onSign] and
 * [onDecline] only when the viewer may answer it.
 */
@Composable
fun ContractCard(
    title: String,
    terms: String,
    status: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    onSign: (() -> Unit)? = null,
    onDecline: (() -> Unit)? = null,
) {
    PaperCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f, fill = false))
            Text(status, style = MaterialTheme.typography.labelLarge, color = VaultTheme.colors.paperInk)
        }
        Text(terms, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = VaultSpacing.xs))
        if (note != null) {
            Text(note, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = VaultSpacing.xs))
        }
        if (onSign != null || onDecline != null) {
            Row(
                Modifier.fillMaxWidth().padding(top = VaultSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
            ) {
                onDecline?.let {
                    VaultButton(
                        stringResource(R.string.contract_decline),
                        onClick = it,
                        style = VaultButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
                onSign?.let {
                    VaultButton(stringResource(R.string.contract_sign), onClick = it, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@ComponentPreviews
@Composable
private fun ContractCardPreview() {
    VaultPreviewTheme {
        Column(Modifier.padding(VaultSpacing.lg), verticalArrangement = Arrangement.spacedBy(VaultSpacing.md)) {
            ContractCard(
                title = "From Ben",
                terms = "Ben gives Asha 50% of this round's winnings",
                status = "Waiting for you",
                note = "Signing costs you 5 coins.",
                onSign = {},
                onDecline = {},
            )
            ContractCard(
                title = "With Chandrika",
                terms = "Asha pays Chandrika 10 coins no matter what",
                status = "Signed",
            )
        }
    }
}
