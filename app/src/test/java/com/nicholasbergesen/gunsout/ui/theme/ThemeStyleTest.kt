package com.nicholasbergesen.gunsout.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ThemeStyleTest {

    @Test fun themeStyles_parseStoredNamesWithSafeDefault() {
        assertEquals(ThemeStyle.SOFT_PASTEL, ThemeStyle.fromStoredName("SOFT_PASTEL"))
        assertEquals(ThemeStyle.Default, ThemeStyle.fromStoredName(null))
        assertEquals(ThemeStyle.Default, ThemeStyle.fromStoredName("NOT_A_THEME"))
    }

    @Test fun colorSchemes_matchMockupTokens() {
        assertThemeColors(
            ThemeStyle.GUNMETAL_CRIMSON,
            background = GunsoutPalette.GunMetalBackground,
            text = GunsoutPalette.IceText,
            primary = GunsoutPalette.CrimsonDark,
            surface = GunsoutPalette.GunMetalSurface,
            line = GunsoutPalette.GunMetalLine
        )
        assertThemeColors(
            ThemeStyle.CLEAN_LIGHT_MINIMAL,
            background = GunsoutPalette.CleanBackground,
            text = GunsoutPalette.CleanText,
            primary = GunsoutPalette.CleanAccent,
            surface = GunsoutPalette.CleanCard,
            line = GunsoutPalette.CleanLine
        )
        assertThemeColors(
            ThemeStyle.NEO_BRUTALIST,
            background = GunsoutPalette.NeoBackground,
            text = GunsoutPalette.NeoText,
            primary = GunsoutPalette.NeoAccent,
            surface = GunsoutPalette.NeoCard,
            line = GunsoutPalette.NeoText
        )
        assertThemeColors(
            ThemeStyle.GLASSMORPHISM,
            background = GunsoutPalette.GlassBackgroundEnd,
            text = GunsoutPalette.GlassText,
            primary = GunsoutPalette.GlassAccent,
            surface = GunsoutPalette.GlassCard,
            line = GunsoutPalette.GlassLine
        )
        assertThemeColors(
            ThemeStyle.SOFT_PASTEL,
            background = GunsoutPalette.SoftBackgroundStart,
            text = GunsoutPalette.SoftText,
            primary = GunsoutPalette.SoftAccent,
            surface = GunsoutPalette.SoftCard,
            line = GunsoutPalette.SoftLine
        )
        assertThemeColors(
            ThemeStyle.VIBRANT_GRADIENT,
            background = GunsoutPalette.VibrantBackgroundStart,
            text = GunsoutPalette.VibrantText,
            primary = GunsoutPalette.VibrantAccent,
            surface = GunsoutPalette.VibrantCard,
            line = GunsoutPalette.VibrantLine
        )
    }

    @Test fun shapes_matchMockupRadii() {
        assertThemeShapes(ThemeStyle.GUNMETAL_CRIMSON, cardRadius = 12, buttonRadius = 10)
        assertThemeShapes(ThemeStyle.CLEAN_LIGHT_MINIMAL, cardRadius = 18, buttonRadius = 14)
        assertThemeShapes(ThemeStyle.NEO_BRUTALIST, cardRadius = 4, buttonRadius = 4)
        assertThemeShapes(ThemeStyle.GLASSMORPHISM, cardRadius = 20, buttonRadius = 16)
        assertThemeShapes(ThemeStyle.SOFT_PASTEL, cardRadius = 24, buttonRadius = 20)
        assertThemeShapes(ThemeStyle.VIBRANT_GRADIENT, cardRadius = 22, buttonRadius = 18)
    }

    @Test fun everyThemeProvidesABackdropBrush() {
        ThemeStyle.values().forEach { style ->
            assertNotNull(backdropBrushFor(style, Size(width = 360f, height = 800f)))
        }
    }

    private fun assertThemeColors(
        style: ThemeStyle,
        background: Color,
        text: Color,
        primary: Color,
        surface: Color,
        line: Color
    ) {
        val scheme = colorSchemeFor(style)
        assertEquals(background, scheme.background)
        assertEquals(text, scheme.onBackground)
        assertEquals(primary, scheme.primary)
        assertEquals(surface, scheme.surface)
        assertEquals(line, scheme.outline)
    }

    private fun assertThemeShapes(
        style: ThemeStyle,
        cardRadius: Int,
        buttonRadius: Int
    ) {
        val shapes = shapesFor(style)
        assertEquals(RoundedCornerShape(cardRadius.dp), shapes.medium)
        assertEquals(RoundedCornerShape(buttonRadius.dp), shapes.small)
    }
}
