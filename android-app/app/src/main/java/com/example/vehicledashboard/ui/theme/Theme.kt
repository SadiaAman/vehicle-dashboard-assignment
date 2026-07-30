package com.example.vehicledashboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * The app theme.
 *
 * Two deliberate changes to the Android Studio template:
 *
 * 1. **Dark only.** A vehicle dashboard is looked at from the driver's seat,
 *    often at night. A light scheme would be glaring, so there is no light
 *    variant and the system setting is ignored.
 * 2. **No dynamic colour.** Material You would recolour the dashboard from the
 *    user's wallpaper. That is lovely for a phone and wrong here: the meaning
 *    of green ("charging", "battery healthy") must never change, and contrast
 *    must not depend on a wallpaper.
 */
private val DashboardColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextPrimary,
    secondary = AccentGreen,
    onSecondary = DashboardBackground,
    tertiary = AccentAmber,
    error = AccentRed,
    background = DashboardBackground,
    onBackground = TextPrimary,
    surface = DashboardSurface,
    onSurface = TextPrimary,
    surfaceVariant = DashboardSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextDisabled,
)

@Composable
fun VehicleDashboardTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DashboardColorScheme,
        typography = Typography,
        content = content,
    )
}
