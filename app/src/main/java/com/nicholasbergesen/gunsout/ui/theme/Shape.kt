package com.nicholasbergesen.gunsout.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val GunsoutShapes: Shapes = shapesFor(ThemeStyle.Default)

internal fun shapesFor(style: ThemeStyle): Shapes = when (style) {
    ThemeStyle.GUNMETAL_CRIMSON -> themeShapes(cardRadius = 12.dp, buttonRadius = 8.dp)
    ThemeStyle.CLEAN_LIGHT_MINIMAL -> themeShapes(cardRadius = 18.dp, buttonRadius = 8.dp)
    ThemeStyle.NEO_BRUTALIST -> themeShapes(cardRadius = 4.dp, buttonRadius = 4.dp, extraLargeRadius = 6.dp)
    ThemeStyle.GLASSMORPHISM -> themeShapes(cardRadius = 20.dp, buttonRadius = 10.dp, extraLargeRadius = 28.dp)
    ThemeStyle.SOFT_PASTEL -> themeShapes(cardRadius = 24.dp, buttonRadius = 12.dp, extraLargeRadius = 32.dp)
    ThemeStyle.VIBRANT_GRADIENT -> themeShapes(cardRadius = 22.dp, buttonRadius = 12.dp, extraLargeRadius = 30.dp)
}

private fun themeShapes(
    cardRadius: Dp,
    buttonRadius: Dp,
    extraLargeRadius: Dp = cardRadius + 8.dp
) = Shapes(
    extraSmall = RoundedCornerShape((buttonRadius - 6.dp).coerceAtLeast(2.dp)),
    small = RoundedCornerShape(buttonRadius),
    medium = RoundedCornerShape(cardRadius),
    large = RoundedCornerShape(cardRadius),
    extraLarge = RoundedCornerShape(extraLargeRadius)
)
