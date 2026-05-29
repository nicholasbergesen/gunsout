package com.nicholasbergesen.gunsout.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GunMetalDark = darkColorScheme(
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
    outline = GunsoutPalette.SteelOutline,
    outlineVariant = GunsoutPalette.SteelOutline,
    error = GunsoutPalette.CrimsonError,
    onError = Color.Black,
    errorContainer = GunsoutPalette.CrimsonDeep,
    onErrorContainer = GunsoutPalette.CrimsonSoft
)

private val GunMetalLight = lightColorScheme(
    primary = GunsoutPalette.CrimsonLight,
    onPrimary = Color.White,
    primaryContainer = GunsoutPalette.CrimsonSoft,
    onPrimaryContainer = GunsoutPalette.CrimsonDeep,
    secondary = GunsoutPalette.SteelSecondary,
    onSecondary = Color.White,
    secondaryContainer = GunsoutPalette.PewterSurfaceVariant,
    onSecondaryContainer = GunsoutPalette.InkText,
    tertiary = GunsoutPalette.CrimsonDeep,
    onTertiary = Color.White,
    background = GunsoutPalette.PewterBackground,
    onBackground = GunsoutPalette.InkText,
    surface = GunsoutPalette.PewterSurface,
    onSurface = GunsoutPalette.InkText,
    surfaceVariant = GunsoutPalette.PewterSurfaceVariant,
    onSurfaceVariant = GunsoutPalette.SlateText,
    surfaceContainer = GunsoutPalette.PewterSurfaceVariant,
    surfaceContainerHigh = GunsoutPalette.PewterSurfaceVariant,
    surfaceContainerHighest = GunsoutPalette.PewterSurfaceVariant,
    outline = GunsoutPalette.PewterOutline,
    outlineVariant = GunsoutPalette.PewterOutline,
    error = GunsoutPalette.CrimsonLight,
    onError = Color.White,
    errorContainer = GunsoutPalette.CrimsonSoft,
    onErrorContainer = GunsoutPalette.CrimsonDeep
)

@Composable
fun GunsoutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) GunMetalDark else GunMetalLight
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = GunsoutTypography,
        shapes = GunsoutShapes,
        content = content
    )
}
