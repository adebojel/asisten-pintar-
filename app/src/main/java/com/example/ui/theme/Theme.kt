package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WhiteColorScheme = lightColorScheme(
    primary = WhiteThemePrimary,
    onPrimary = WhiteThemeOnPrimary,
    primaryContainer = WhiteThemePrimaryContainer,
    onPrimaryContainer = WhiteThemeOnPrimaryContainer,
    secondary = WhiteThemeSecondary,
    onSecondary = WhiteThemeOnSecondary,
    secondaryContainer = WhiteThemeSecondaryContainer,
    onSecondaryContainer = WhiteThemeOnSecondaryContainer,
    tertiary = WhiteThemeTertiary,
    background = WhiteThemeBackground,
    surface = WhiteThemeSurface,
    surfaceVariant = WhiteThemeSurfaceVariant,
    outline = WhiteThemeOutline,
    outlineVariant = WhiteThemeOutlineVariant,
    onBackground = WhiteThemeTextPrimary,
    onSurface = WhiteThemeTextPrimary,
    onSurfaceVariant = WhiteThemeTextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF34D399),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    outline = Color(0xFF475569),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Set to false to enforce the White Theme requested by user
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else WhiteColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
