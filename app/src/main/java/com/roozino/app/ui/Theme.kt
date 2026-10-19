package com.roozino.app.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.roozino.app.R

enum class ThemeMode { SYSTEM, LIGHT, DARK }

object SettingsStore {
    private const val PREFS = "roozino_settings"
    private const val KEY_THEME = "theme_mode"

    fun themeMode(context: Context): ThemeMode {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, mode.name).apply()
    }
}

// پالت گرم «طلوع» — بدون آبی و بنفش
private val LightColors = lightColorScheme(
    primary = Color(0xFFAD4E12),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF361000),
    secondary = Color(0xFF755846),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCC7),
    onSecondaryContainer = Color(0xFF2B160A),
    tertiary = Color(0xFF626031),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE9E5AB),
    onTertiaryContainer = Color(0xFF1D1D00),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A14),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A14),
    surfaceVariant = Color(0xFFF4DED1),
    onSurfaceVariant = Color(0xFF52443A),
    outline = Color(0xFF857468),
    outlineVariant = Color(0xFFD7C2B5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68B),
    onPrimary = Color(0xFF542200),
    primaryContainer = Color(0xFF773400),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFE5BFA6),
    onSecondary = Color(0xFF432B1B),
    secondaryContainer = Color(0xFF5C4130),
    onSecondaryContainer = Color(0xFFFFDCC7),
    tertiary = Color(0xFFCCC98F),
    onTertiary = Color(0xFF333100),
    tertiaryContainer = Color(0xFF4A4800),
    onTertiaryContainer = Color(0xFFE9E5AB),
    background = Color(0xFF191210),
    onBackground = Color(0xFFEFDFD4),
    surface = Color(0xFF191210),
    onSurface = Color(0xFFEFDFD4),
    surfaceVariant = Color(0xFF52443A),
    onSurfaceVariant = Color(0xFFD7C2B5),
    outline = Color(0xFF9F8D80),
    outlineVariant = Color(0xFF52443A)
)

private val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private fun persianTypography(): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Vazirmatn),
        displayMedium = base.displayMedium.copy(fontFamily = Vazirmatn),
        displaySmall = base.displaySmall.copy(fontFamily = Vazirmatn),
        headlineLarge = base.headlineLarge.copy(fontFamily = Vazirmatn),
        headlineMedium = base.headlineMedium.copy(fontFamily = Vazirmatn),
        headlineSmall = base.headlineSmall.copy(fontFamily = Vazirmatn),
        titleLarge = base.titleLarge.copy(fontFamily = Vazirmatn),
        titleMedium = base.titleMedium.copy(fontFamily = Vazirmatn),
        titleSmall = base.titleSmall.copy(fontFamily = Vazirmatn),
        bodyLarge = base.bodyLarge.copy(fontFamily = Vazirmatn),
        bodyMedium = base.bodyMedium.copy(fontFamily = Vazirmatn),
        bodySmall = base.bodySmall.copy(fontFamily = Vazirmatn),
        labelLarge = base.labelLarge.copy(fontFamily = Vazirmatn),
        labelMedium = base.labelMedium.copy(fontFamily = Vazirmatn),
        labelSmall = base.labelSmall.copy(fontFamily = Vazirmatn)
    )
}

@Composable
fun RoozinoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = persianTypography(),
        content = content
    )
}
