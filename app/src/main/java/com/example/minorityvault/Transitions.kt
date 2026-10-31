package com.example.minorityvault

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

internal fun instant(): ContentTransform = fadeIn(snap()) togetherWith fadeOut(snap())

internal fun quickFade(): ContentTransform =
    fadeIn(tween(QUICK_FADE_MILLIS)) togetherWith fadeOut(tween(QUICK_FADE_MILLIS))

/** The new screen rises into place like a heavy panel; the old one lifts away. */
internal fun heavyPanelSlide(): ContentTransform {
    val rise = slideInVertically(tween(PANEL_MILLIS, easing = HeavyEasing)) { it / PANEL_RISE } + fadeIn()
    val lift = slideOutVertically(tween(PANEL_MILLIS, easing = HeavyEasing)) { -it / PANEL_LIFT } + fadeOut()
    return rise togetherWith lift
}

private val HeavyEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val PANEL_MILLIS = 480
private const val QUICK_FADE_MILLIS = 150
private const val PANEL_RISE = 3
private const val PANEL_LIFT = 6
