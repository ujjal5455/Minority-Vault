package com.example.minorityvault.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.minorityvault.designsystem.GlassSurface
import com.example.minorityvault.designsystem.GlassTint
import com.example.minorityvault.designsystem.VaultPreviewTheme
import com.example.minorityvault.designsystem.VaultSpacing
import com.example.minorityvault.designsystem.glassRim
import com.example.minorityvault.designsystem.preview.ComponentPreviews

/** Frosted paper, for anything official: questions and contracts. Dark text on light glass. */
@Composable
fun PaperCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(modifier = modifier, tint = GlassTint.Paper, rim = glassRim(strength = PAPER_RIM_STRENGTH)) {
        Column(Modifier.padding(VaultSpacing.lg), content = content)
    }
}

private const val PAPER_RIM_STRENGTH = 1.6f

@ComponentPreviews
@Composable
private fun PaperCardPreview() {
    VaultPreviewTheme {
        PaperCard(Modifier.padding(VaultSpacing.lg)) {
            Text("Round 3 of 10", style = MaterialTheme.typography.labelLarge)
            Text("Is a hot dog a sandwich?", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
