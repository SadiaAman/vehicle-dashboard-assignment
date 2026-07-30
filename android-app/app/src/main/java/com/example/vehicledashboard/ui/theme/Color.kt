package com.example.vehicledashboard.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Dashboard palette.
 *
 * Chosen for a driver, not for decoration:
 * - very dark backgrounds, because a bright screen at night is dangerous
 * - one accent colour (blue) for "information", one (green) for "good state",
 *   one (amber/red) for "attention" - a driver should decode colour instantly
 * - white on near-black for the numbers that must be readable at a glance
 *
 * The palette is generic on purpose: no vehicle manufacturer's brand colours.
 */

// Surfaces, darkest to lightest
val DashboardBackground = Color(0xFF090C10)
val DashboardSurface = Color(0xFF141A22)
val DashboardSurfaceVariant = Color(0xFF1E2732)

// Accents
val AccentBlue = Color(0xFF2E7DF7)
val AccentGreen = Color(0xFF34C759)
val AccentAmber = Color(0xFFFFB020)
val AccentRed = Color(0xFFFF453A)

// Text
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF9AA5B1)
val TextDisabled = Color(0xFF5A6570)
