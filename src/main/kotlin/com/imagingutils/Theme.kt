package com.imagingutils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** How the app picks its color scheme: follow the OS, or force light/dark. */
enum class ThemeMode(val label: String) {
    SYSTEM("🖥 System"),
    LIGHT("☀ Light"),
    DARK("🌙 Dark");

    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}

/** Applies the app's Material theme, resolving [themeMode] to a light/dark scheme. */
@Composable
fun AppTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
