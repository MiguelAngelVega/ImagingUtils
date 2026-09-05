package com.imagingutils

/** How the app picks its color scheme: follow the OS, or force light/dark. */
enum class ThemeMode(val label: String) {
    SYSTEM("🖥 System"),
    LIGHT("☀ Light"),
    DARK("🌙 Dark");

    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}
