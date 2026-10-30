package com.example.minorityvault.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Big Shoulders Display (OFL): condensed, for numbers and headlines. */
val DisplayFontFamily = FontFamily(
    variableFont(R.font.big_shoulders_display, FontWeight.SemiBold),
    variableFont(R.font.big_shoulders_display, FontWeight.Bold),
    variableFont(R.font.big_shoulders_display, FontWeight.ExtraBold),
    variableFont(R.font.big_shoulders_display, FontWeight.Black),
)

/** Instrument Sans (OFL): clean sans for UI text. */
val UiFontFamily = FontFamily(
    variableFont(R.font.instrument_sans, FontWeight.Normal),
    variableFont(R.font.instrument_sans, FontWeight.Medium),
    variableFont(R.font.instrument_sans, FontWeight.SemiBold),
    variableFont(R.font.instrument_sans, FontWeight.Bold),
)

private fun display(weight: FontWeight, size: Int, line: Int) =
    TextStyle(fontFamily = DisplayFontFamily, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private fun ui(weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = UiFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

val VaultTypography = Typography(
    displayLarge = display(FontWeight.Black, size = 72, line = 72),
    displayMedium = display(FontWeight.Black, size = 52, line = 54),
    displaySmall = display(FontWeight.ExtraBold, size = 40, line = 44),
    headlineLarge = display(FontWeight.ExtraBold, size = 34, line = 38),
    headlineMedium = display(FontWeight.Bold, size = 28, line = 32),
    headlineSmall = display(FontWeight.Bold, size = 24, line = 28),
    titleLarge = ui(FontWeight.SemiBold, size = 20, line = 26),
    titleMedium = ui(FontWeight.SemiBold, size = 16, line = 22),
    titleSmall = ui(FontWeight.Medium, size = 14, line = 20),
    bodyLarge = ui(FontWeight.Normal, size = 16, line = 24),
    bodyMedium = ui(FontWeight.Normal, size = 14, line = 20),
    bodySmall = ui(FontWeight.Normal, size = 12, line = 16),
    labelLarge = ui(FontWeight.SemiBold, size = 14, line = 20, tracking = 0.2),
    labelMedium = ui(FontWeight.SemiBold, size = 12, line = 16, tracking = 0.3),
    labelSmall = ui(FontWeight.Medium, size = 11, line = 16, tracking = 0.3),
)
