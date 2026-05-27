package com.gunsout.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.gunsout.R

// Inter is fetched through the Google Fonts provider. The bundled fallback chain
// resolves to the system default if the device has no network and the cached
// font is not yet warm.
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val inter = GoogleFont("Inter")

private fun interFont(weight: FontWeight) =
    Font(googleFont = inter, fontProvider = googleFontProvider, weight = weight, style = FontStyle.Normal)

val InterFamily = FontFamily(
    interFont(FontWeight.Normal),
    interFont(FontWeight.Medium),
    interFont(FontWeight.SemiBold),
    interFont(FontWeight.Bold)
)

// Tabular numerics for set counters and macro readouts so digits do not jitter.
val TabularNumStyle = TextStyle(fontFeatureSettings = "tnum")

private fun base(weight: FontWeight, size: Int, lineHeight: Int, letterSpacing: Double = 0.0) =
    TextStyle(
        fontFamily = InterFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.sp
    )

val GunsoutTypography = Typography(
    displayLarge = base(FontWeight.Bold, 40, 48, letterSpacing = -0.5),
    displayMedium = base(FontWeight.Bold, 32, 40, letterSpacing = -0.25),
    displaySmall = base(FontWeight.SemiBold, 28, 36),
    headlineLarge = base(FontWeight.Bold, 26, 34, letterSpacing = -0.25),
    headlineMedium = base(FontWeight.SemiBold, 22, 30),
    headlineSmall = base(FontWeight.SemiBold, 20, 28),
    titleLarge = base(FontWeight.SemiBold, 18, 26),
    titleMedium = base(FontWeight.SemiBold, 16, 22),
    titleSmall = base(FontWeight.Medium, 14, 20),
    bodyLarge = base(FontWeight.Normal, 16, 24),
    bodyMedium = base(FontWeight.Normal, 14, 20),
    bodySmall = base(FontWeight.Normal, 12, 16),
    labelLarge = base(FontWeight.SemiBold, 14, 20),
    labelMedium = base(FontWeight.Medium, 12, 16),
    labelSmall = base(FontWeight.Medium, 11, 14)
)
