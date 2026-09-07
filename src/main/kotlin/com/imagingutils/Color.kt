package com.imagingutils

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Color palette and Material color schemes, kept separate from the UI code so
 * the app's look can be tuned in one place. Only the roles that differ from the
 * Material 3 defaults are overridden here; the rest are derived automatically.
 */

// Brand palette.
private val BrandPrimary = Color(0xFF3D5AFE)
private val BrandPrimaryLight = Color(0xFF8C9EFF)

val LightColors = lightColorScheme(
    primary = BrandPrimary,
)

val DarkColors = darkColorScheme(
    primary = BrandPrimaryLight,
)
