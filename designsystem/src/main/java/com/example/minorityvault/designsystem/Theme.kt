package com.example.minorityvault.designsystem

import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Spacing scale. Use these instead of raw dp values for padding and gaps. */
object VaultSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Minimum size of anything tappable (CLAUDE.md §9). */
val MinTouchTarget = 48.dp

/** Seconds left at which timers turn stamp red and start to pulse. */
const val URGENT_SECONDS = 10

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
    surfaceContainer = PanelGreen,
    surfaceContainerHigh = PanelGreenRaised,
    outline = Steel,
    error = StampRed,
    onError = AgedPaper,
)

internal val VaultShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

internal val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * The game's theme. [reducedMotion] defaults to the system setting (animations turned off in developer or
 * accessibility settings); components skip spins, pulses and slams when it is true.
 */
@Composable
fun MinorityVaultTheme(reducedMotion: Boolean? = null, content: @Composable () -> Unit) {
    val systemReducedMotion = rememberSystemReducedMotion()
    CompositionLocalProvider(
        LocalVaultColors provides VaultColors(),
        LocalReducedMotion provides (reducedMotion ?: systemReducedMotion),
    ) {
        MaterialTheme(
            colorScheme = VaultColorScheme,
            typography = VaultTypography,
            shapes = VaultShapes,
            content = content,
        )
    }
}

object VaultTheme {
    val colors: VaultColors
        @Composable @ReadOnlyComposable
        get() = LocalVaultColors.current

    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReducedMotion.current
}

@Composable
private fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}
