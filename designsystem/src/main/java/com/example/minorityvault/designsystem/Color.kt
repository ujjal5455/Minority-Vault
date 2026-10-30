package com.example.minorityvault.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Banknote and vault palette (CLAUDE.md §8).
val InkGreen = Color(0xFF0F2B27)
val PanelGreen = Color(0xFF173B35)
val PanelGreenRaised = Color(0xFF1E4740)
val AgedPaper = Color(0xFFEAE3CF)
val PaperInk = Color(0xFF22302C)
val PaperEdge = Color(0xFFCBBF9E)
val Brass = Color(0xFFC9A055)
val StampRed = Color(0xFFD2584A)
val Steel = Color(0xFF93A09B)

/**
 * The full palette with roles. Stamp red is for time pressure and betrayal only; brass is for money and
 * primary actions; paper is for anything official (questions, contracts).
 */
@Immutable
data class VaultColors(
    val ink: Color = InkGreen,
    val panel: Color = PanelGreen,
    val panelRaised: Color = PanelGreenRaised,
    val paper: Color = AgedPaper,
    val paperInk: Color = PaperInk,
    val paperEdge: Color = PaperEdge,
    val brass: Color = Brass,
    val stampRed: Color = StampRed,
    val steel: Color = Steel,
    val onPanel: Color = AgedPaper,
    val onPanelMuted: Color = Steel,
)

internal val LocalVaultColors = staticCompositionLocalOf { VaultColors() }
