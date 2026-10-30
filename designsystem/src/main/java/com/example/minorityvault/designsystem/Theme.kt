package com.example.minorityvault.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Banknote and vault palette (CLAUDE.md §8). Fonts and components arrive in Phase 2.
val InkGreen = Color(0xFF0F2B27)
val PanelGreen = Color(0xFF173B35)
val PanelGreenRaised = Color(0xFF1E4740)
val AgedPaper = Color(0xFFEAE3CF)
val Brass = Color(0xFFC9A055)
val StampRed = Color(0xFFD2584A)
val Steel = Color(0xFF93A09B)

private val VaultColorScheme = darkColorScheme(
    primary = Brass,
    onPrimary = InkGreen,
    secondary = Steel,
    onSecondary = InkGreen,
    background = InkGreen,
    onBackground = AgedPaper,
    surface = PanelGreen,
    onSurface = AgedPaper,
    surfaceVariant = PanelGreenRaised,
    onSurfaceVariant = Steel,
    error = StampRed,
    onError = AgedPaper,
)

@Composable
fun MinorityVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VaultColorScheme, content = content)
}
