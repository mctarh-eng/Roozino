package com.roozino.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.isSystemInDarkTheme
import com.roozino.app.R

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

@Composable
fun RoozinoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkColors else LightColors
    val base = remember { Typography(fontFamily = Vazirmatn) }
    val scaled = remember(fontScale) {
        if (fontScale == 1f) base else Typography(
            displayLarge = base.displayLarge.copy(fontSize = base.displayLarge.fontSize * fontScale),
            displayMedium = base.displayMedium.copy(fontSize = base.displayMedium.fontSize * fontScale),
            displaySmall = base.displaySmall.copy(fontSize = base.displaySmall.fontSize * fontScale),
            headlineLarge = base.headlineLarge.copy(fontSize = base.headlineLarge.fontSize * fontScale),
            headlineMedium = base.headlineMedium.copy(fontSize = base.headlineMedium.fontSize * fontScale),
            headlineSmall = base.headlineSmall.copy(fontSize = base.headlineSmall.fontSize * fontScale),
            titleLarge = base.titleLarge.copy(fontSize = base.titleLarge.fontSize * fontScale),
            titleMedium = base.titleMedium.copy(fontSize = base.titleMedium.fontSize * fontScale),
            titleSmall = base.titleSmall.copy(fontSize = base.titleSmall.fontSize * fontScale),
            bodyLarge = base.bodyLarge.copy(fontSize = base.bodyLarge.fontSize * fontScale),
            bodyMedium = base.bodyMedium.copy(fontSize = base.bodyMedium.fontSize * fontScale),
            bodySmall = base.bodySmall.copy(fontSize = base.bodySmall.fontSize * fontScale),
            labelLarge = base.labelLarge.copy(fontSize = base.labelLarge.fontSize * fontScale),
            labelMedium = base.labelMedium.copy(fontSize = base.labelMedium.fontSize * fontScale),
            labelSmall = base.labelSmall.copy(fontSize = base.labelSmall.fontSize * fontScale)
        )
    }
    MaterialTheme(colorScheme = scheme, typography = scaled, content = content)
}
