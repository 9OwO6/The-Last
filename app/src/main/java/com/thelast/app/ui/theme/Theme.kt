package com.thelast.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.thelast.app.data.datastore.ThemeMode

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightSurface,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = Color(0xFF4A3326),
    surface = LightSurface,
    onSurface = Color(0xFF4A3326),
    surfaceContainer = Color(0xFFF0E9DE),
    surfaceContainerHigh = Color(0xFFE6D9C7),
    onSurfaceVariant = Color(0xFF796B60),
    outlineVariant = Color(0xFFE1D7CA),
    error = Color(0xFFD15B5B),
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = Color(0xFFFFF2E4),
    surface = DarkSurface,
    onSurface = Color(0xFFFFF2E4),
    surfaceContainer = Color(0xFF30251D),
    surfaceContainerHigh = Color(0xFF3B2E24),
    onSurfaceVariant = Color(0xFFE2CDB9),
    outlineVariant = Color(0xFF5B4739),
    error = Color(0xFFFFB4AB),
)

@Composable
fun TheLastTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Warm -> false
        ThemeMode.Night -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TheLastTypography,
        content = content,
    )
}
