package com.nicholasbergesen.gunsout.ui.theme

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GunMetalCrimson = darkColorScheme(
    primary = GunsoutPalette.CrimsonDark,
    onPrimary = Color.White,
    primaryContainer = GunsoutPalette.CrimsonDeep,
    onPrimaryContainer = GunsoutPalette.CrimsonSoft,
    secondary = GunsoutPalette.SteelSecondary,
    onSecondary = GunsoutPalette.IceText,
    secondaryContainer = GunsoutPalette.GunMetalSurfaceContainer,
    onSecondaryContainer = GunsoutPalette.IceText,
    tertiary = GunsoutPalette.CrimsonSoft,
    onTertiary = GunsoutPalette.CrimsonDeep,
    background = GunsoutPalette.GunMetalBackground,
    onBackground = GunsoutPalette.IceText,
    surface = GunsoutPalette.GunMetalSurface,
    onSurface = GunsoutPalette.IceText,
    surfaceVariant = GunsoutPalette.GunMetalSurfaceVariant,
    onSurfaceVariant = GunsoutPalette.MutedText,
    surfaceContainer = GunsoutPalette.GunMetalSurfaceContainer,
    surfaceContainerHigh = GunsoutPalette.GunMetalSurfaceVariant,
    surfaceContainerHighest = GunsoutPalette.GunMetalSurfaceVariant,
    outline = GunsoutPalette.GunMetalLine,
    outlineVariant = GunsoutPalette.GunMetalCardBorder,
    error = GunsoutPalette.CrimsonError,
    onError = Color.Black,
    errorContainer = GunsoutPalette.CrimsonDeep,
    onErrorContainer = GunsoutPalette.CrimsonSoft
)

private val CleanLightMinimal = lightColorScheme(
    primary = GunsoutPalette.CleanAccent,
    onPrimary = Color.White,
    primaryContainer = GunsoutPalette.CleanChip,
    onPrimaryContainer = GunsoutPalette.CleanAccent,
    secondary = GunsoutPalette.CleanAccent,
    onSecondary = Color.White,
    secondaryContainer = GunsoutPalette.CleanChip,
    onSecondaryContainer = GunsoutPalette.CleanAccent,
    tertiary = GunsoutPalette.CleanAccent,
    onTertiary = Color.White,
    background = GunsoutPalette.CleanBackground,
    onBackground = GunsoutPalette.CleanText,
    surface = GunsoutPalette.CleanCard,
    onSurface = GunsoutPalette.CleanText,
    surfaceVariant = GunsoutPalette.CleanChip,
    onSurfaceVariant = GunsoutPalette.CleanMuted,
    surfaceContainer = GunsoutPalette.CleanCard,
    surfaceContainerHigh = GunsoutPalette.CleanCard,
    surfaceContainerHighest = GunsoutPalette.CleanLine,
    outline = GunsoutPalette.CleanLine,
    outlineVariant = GunsoutPalette.CleanLine,
    error = GunsoutPalette.CrimsonLight,
    onError = Color.White,
    errorContainer = GunsoutPalette.CrimsonSoft,
    onErrorContainer = GunsoutPalette.CrimsonDeep
)

private val NeoBrutalist = lightColorScheme(
    primary = GunsoutPalette.NeoAccent,
    onPrimary = Color.White,
    primaryContainer = GunsoutPalette.NeoAccentCard,
    onPrimaryContainer = GunsoutPalette.NeoText,
    secondary = GunsoutPalette.NeoText,
    onSecondary = Color.White,
    secondaryContainer = GunsoutPalette.NeoCard,
    onSecondaryContainer = GunsoutPalette.NeoText,
    tertiary = GunsoutPalette.NeoAccentCard,
    onTertiary = GunsoutPalette.NeoText,
    background = GunsoutPalette.NeoBackground,
    onBackground = GunsoutPalette.NeoText,
    surface = GunsoutPalette.NeoCard,
    onSurface = GunsoutPalette.NeoText,
    surfaceVariant = GunsoutPalette.NeoAccentCard,
    onSurfaceVariant = GunsoutPalette.NeoMuted,
    surfaceContainer = GunsoutPalette.NeoCard,
    surfaceContainerHigh = GunsoutPalette.NeoAccentCard,
    surfaceContainerHighest = GunsoutPalette.NeoTrack,
    outline = GunsoutPalette.NeoText,
    outlineVariant = GunsoutPalette.NeoText,
    error = GunsoutPalette.NeoAccent,
    onError = Color.White,
    errorContainer = GunsoutPalette.NeoAccentCard,
    onErrorContainer = GunsoutPalette.NeoText
)

private val Glassmorphism = darkColorScheme(
    primary = GunsoutPalette.GlassAccent,
    onPrimary = GunsoutPalette.GlassOnAccent,
    primaryContainer = GunsoutPalette.GlassAccentCard,
    onPrimaryContainer = GunsoutPalette.GlassText,
    secondary = GunsoutPalette.GlassMuted,
    onSecondary = GunsoutPalette.GlassOnAccent,
    secondaryContainer = GunsoutPalette.GlassChip,
    onSecondaryContainer = GunsoutPalette.GlassText,
    tertiary = GunsoutPalette.GlassAccent,
    onTertiary = GunsoutPalette.GlassOnAccent,
    background = GunsoutPalette.GlassBackgroundEnd,
    onBackground = GunsoutPalette.GlassText,
    surface = GunsoutPalette.GlassCard,
    onSurface = GunsoutPalette.GlassText,
    surfaceVariant = GunsoutPalette.GlassChip,
    onSurfaceVariant = GunsoutPalette.GlassMuted,
    surfaceContainer = GunsoutPalette.GlassCard,
    surfaceContainerHigh = GunsoutPalette.GlassAccentCard,
    surfaceContainerHighest = GunsoutPalette.GlassTrack,
    outline = GunsoutPalette.GlassLine,
    outlineVariant = GunsoutPalette.GlassBorder,
    error = GunsoutPalette.CrimsonError,
    onError = Color.Black,
    errorContainer = GunsoutPalette.CrimsonDeep,
    onErrorContainer = GunsoutPalette.CrimsonSoft
)

private val SoftPastel = lightColorScheme(
    primary = GunsoutPalette.SoftAccent,
    onPrimary = Color.White,
    primaryContainer = GunsoutPalette.SoftAccentCardStart,
    onPrimaryContainer = GunsoutPalette.SoftAccentDark,
    secondary = GunsoutPalette.SoftAccentDark,
    onSecondary = Color.White,
    secondaryContainer = GunsoutPalette.SoftChip,
    onSecondaryContainer = GunsoutPalette.SoftAccentDark,
    tertiary = GunsoutPalette.SoftAccentCardEnd,
    onTertiary = GunsoutPalette.SoftText,
    background = GunsoutPalette.SoftBackgroundStart,
    onBackground = GunsoutPalette.SoftText,
    surface = GunsoutPalette.SoftCard,
    onSurface = GunsoutPalette.SoftText,
    surfaceVariant = GunsoutPalette.SoftChip,
    onSurfaceVariant = GunsoutPalette.SoftMuted,
    surfaceContainer = GunsoutPalette.SoftCard,
    surfaceContainerHigh = GunsoutPalette.SoftAccentCardStart,
    surfaceContainerHighest = GunsoutPalette.SoftTrack,
    outline = GunsoutPalette.SoftLine,
    outlineVariant = GunsoutPalette.SoftLine,
    error = GunsoutPalette.CrimsonLight,
    onError = Color.White,
    errorContainer = GunsoutPalette.CrimsonSoft,
    onErrorContainer = GunsoutPalette.CrimsonDeep
)

private val VibrantGradient = darkColorScheme(
    primary = GunsoutPalette.VibrantAccent,
    onPrimary = Color.White,
    primaryContainer = GunsoutPalette.VibrantAccentCard,
    onPrimaryContainer = GunsoutPalette.VibrantText,
    secondary = GunsoutPalette.VibrantText,
    onSecondary = GunsoutPalette.VibrantBackgroundStart,
    secondaryContainer = GunsoutPalette.VibrantChip,
    onSecondaryContainer = GunsoutPalette.VibrantText,
    tertiary = GunsoutPalette.VibrantAccent,
    onTertiary = Color.White,
    background = GunsoutPalette.VibrantBackgroundStart,
    onBackground = GunsoutPalette.VibrantText,
    surface = GunsoutPalette.VibrantCard,
    onSurface = GunsoutPalette.VibrantText,
    surfaceVariant = GunsoutPalette.VibrantChip,
    onSurfaceVariant = GunsoutPalette.VibrantMuted,
    surfaceContainer = GunsoutPalette.VibrantCard,
    surfaceContainerHigh = GunsoutPalette.VibrantAccentCard,
    surfaceContainerHighest = GunsoutPalette.VibrantTrack,
    outline = GunsoutPalette.VibrantLine,
    outlineVariant = GunsoutPalette.VibrantBorder,
    error = GunsoutPalette.CrimsonError,
    onError = Color.Black,
    errorContainer = GunsoutPalette.CrimsonDeep,
    onErrorContainer = GunsoutPalette.CrimsonSoft
)

@Composable
fun GunsoutTheme(
    style: ThemeStyle = ThemeStyle.Default,
    content: @Composable () -> Unit
) {
    val colors = colorSchemeFor(style)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !style.isDark
            controller.isAppearanceLightNavigationBars = !style.isDark
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = GunsoutTypography,
        shapes = shapesFor(style)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val backdrop = backdropBrushFor(style, size)
                    onDrawBehind { drawRect(backdrop) }
                },
            color = Color.Transparent,
            contentColor = colors.onBackground
        ) {
            content()
        }
    }
}

internal fun colorSchemeFor(style: ThemeStyle) = when (style) {
    ThemeStyle.GUNMETAL_CRIMSON -> GunMetalCrimson
    ThemeStyle.CLEAN_LIGHT_MINIMAL -> CleanLightMinimal
    ThemeStyle.NEO_BRUTALIST -> NeoBrutalist
    ThemeStyle.GLASSMORPHISM -> Glassmorphism
    ThemeStyle.SOFT_PASTEL -> SoftPastel
    ThemeStyle.VIBRANT_GRADIENT -> VibrantGradient
}

internal fun backdropBrushFor(style: ThemeStyle, size: androidx.compose.ui.geometry.Size): Brush = when (style) {
    ThemeStyle.GUNMETAL_CRIMSON -> Brush.verticalGradient(
        colors = listOf(GunsoutPalette.GunMetalBackground, GunsoutPalette.GunMetalBackgroundEnd),
        startY = 0f,
        endY = size.height
    )
    ThemeStyle.CLEAN_LIGHT_MINIMAL -> Brush.linearGradient(
        colors = listOf(GunsoutPalette.CleanBackground, GunsoutPalette.CleanBackground),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
    ThemeStyle.NEO_BRUTALIST -> Brush.linearGradient(
        colors = listOf(GunsoutPalette.NeoBackground, GunsoutPalette.NeoBackground),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
    ThemeStyle.GLASSMORPHISM -> Brush.radialGradient(
        colorStops = arrayOf(
            0f to GunsoutPalette.GlassBackgroundStart,
            0.45f to GunsoutPalette.GlassBackgroundMid,
            1f to GunsoutPalette.GlassBackgroundEnd
        ),
        center = Offset(size.width * 0.1f, 0f),
        radius = maxOf(size.width * 1.2f, size.height * 0.8f)
    )
    ThemeStyle.SOFT_PASTEL -> Brush.verticalGradient(
        colors = listOf(GunsoutPalette.SoftBackgroundStart, GunsoutPalette.SoftBackgroundEnd),
        startY = 0f,
        endY = size.height
    )
    ThemeStyle.VIBRANT_GRADIENT -> Brush.linearGradient(
        colorStops = arrayOf(
            0f to GunsoutPalette.VibrantBackgroundStart,
            0.55f to GunsoutPalette.VibrantBackgroundMid,
            1f to GunsoutPalette.VibrantBackgroundEnd
        ),
        start = Offset.Zero,
        end = Offset(size.width * 0.35f, size.height)
    )
}
