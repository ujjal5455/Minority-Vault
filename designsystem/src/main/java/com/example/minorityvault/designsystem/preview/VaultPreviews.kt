package com.example.minorityvault.designsystem.preview

import androidx.compose.ui.tooling.preview.Preview

private const val INK_GREEN = 0xFF0F2B27

/** Screens: a small phone, a large phone, and a small phone at 1.3x font scale. */
@Preview(name = "Small phone", widthDp = 360, heightDp = 640, showBackground = true, backgroundColor = INK_GREEN)
@Preview(name = "Large phone", widthDp = 412, heightDp = 915, showBackground = true, backgroundColor = INK_GREEN)
@Preview(
    name = "Small phone, font 1.3x",
    widthDp = 360,
    heightDp = 640,
    fontScale = 1.3f,
    showBackground = true,
    backgroundColor = INK_GREEN,
)
annotation class ScreenPreviews

/** Components: normal and 1.3x font scale at small-phone width. */
@Preview(name = "Component", widthDp = 360, showBackground = true, backgroundColor = INK_GREEN)
@Preview(
    name = "Component, font 1.3x",
    widthDp = 360,
    fontScale = 1.3f,
    showBackground = true,
    backgroundColor = INK_GREEN,
)
annotation class ComponentPreviews
