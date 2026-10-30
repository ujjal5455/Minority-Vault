package com.example.minorityvault.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.minorityvault.designsystem.MinorityVaultTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.VaultTheme
import com.example.minorityvault.designsystem.preview.ComponentPreviews

/** Aged paper, for anything official: questions and contracts. */
@Composable
fun PaperCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = VaultTheme.colors
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = colors.paper,
        contentColor = colors.paperInk,
        border = BorderStroke(1.dp, colors.paperEdge),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(VaultSpacing.lg), content = content)
    }
}

@ComponentPreviews
@Composable
private fun PaperCardPreview() {
    MinorityVaultTheme {
        PaperCard(Modifier.padding(VaultSpacing.lg)) {
            Text("Round 3 of 10", style = MaterialTheme.typography.labelLarge)
            Text("Is a hot dog a sandwich?", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
