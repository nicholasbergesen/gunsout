package com.nicholasbergesen.gunsout.ui.theme

import androidx.compose.ui.graphics.Color

enum class ThemeStyle(
    val displayName: String,
    val description: String,
    val isDark: Boolean,
    val swatchBackground: Color,
    val swatchBackgroundEnd: Color,
    val swatchSurface: Color,
    val swatchAccent: Color
) {
    GUNMETAL_CRIMSON(
        displayName = "Gunmetal Crimson",
        description = "Polished dark steel, sharp corners, crimson edge",
        isDark = true,
        swatchBackground = GunsoutPalette.GunMetalBackground,
        swatchBackgroundEnd = GunsoutPalette.GunMetalBackgroundEnd,
        swatchSurface = GunsoutPalette.GunMetalSurface,
        swatchAccent = GunsoutPalette.CrimsonDark
    ),
    CLEAN_LIGHT_MINIMAL(
        displayName = "Clean Light Minimal",
        description = "Airy white, indigo accent, soft hairlines",
        isDark = false,
        swatchBackground = GunsoutPalette.CleanBackground,
        swatchBackgroundEnd = GunsoutPalette.CleanBackground,
        swatchSurface = GunsoutPalette.CleanCard,
        swatchAccent = GunsoutPalette.CleanAccent
    ),
    NEO_BRUTALIST(
        displayName = "Neo-Brutalist",
        description = "Hard borders, offset shadows, max contrast",
        isDark = false,
        swatchBackground = GunsoutPalette.NeoBackground,
        swatchBackgroundEnd = GunsoutPalette.NeoBackground,
        swatchSurface = GunsoutPalette.NeoCard,
        swatchAccent = GunsoutPalette.NeoAccent
    ),
    GLASSMORPHISM(
        displayName = "Glassmorphism",
        description = "Frosted translucent cards on aurora blur",
        isDark = true,
        swatchBackground = GunsoutPalette.GlassBackgroundStart,
        swatchBackgroundEnd = GunsoutPalette.GlassBackgroundEnd,
        swatchSurface = GunsoutPalette.GlassCard,
        swatchAccent = GunsoutPalette.GlassAccent
    ),
    SOFT_PASTEL(
        displayName = "Soft Pastel",
        description = "Friendly, rounded, calm mint and peach",
        isDark = false,
        swatchBackground = GunsoutPalette.SoftBackgroundStart,
        swatchBackgroundEnd = GunsoutPalette.SoftBackgroundEnd,
        swatchSurface = GunsoutPalette.SoftCard,
        swatchAccent = GunsoutPalette.SoftAccent
    ),
    VIBRANT_GRADIENT(
        displayName = "Vibrant Gradient",
        description = "Purple to pink energy, glassy cards, rounded",
        isDark = true,
        swatchBackground = GunsoutPalette.VibrantBackgroundStart,
        swatchBackgroundEnd = GunsoutPalette.VibrantBackgroundEnd,
        swatchSurface = GunsoutPalette.VibrantCard,
        swatchAccent = GunsoutPalette.VibrantAccent
    );

    companion object {
        val Default = GUNMETAL_CRIMSON

        fun fromStoredName(value: String?): ThemeStyle =
            value?.let { runCatching { valueOf(it) }.getOrNull() } ?: Default
    }
}
