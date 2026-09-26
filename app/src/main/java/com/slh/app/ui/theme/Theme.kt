package com.slh.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NightColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimaryContainer,
    secondary = NightSecondary,
    onSecondary = NightOnSecondary,
    tertiary = NightTertiary,
    onTertiary = NightOnTertiary,
    error = NightError,
    onError = NightOnError,
    background = NightBackground,
    onBackground = NightTextPrimary,
    surface = NightSurface,
    onSurface = NightTextPrimary,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightTextSecondary,
    outline = NightBorder
)

private val DayColorScheme = lightColorScheme(
    primary = DayPrimary,
    onPrimary = DayOnPrimary,
    primaryContainer = DayPrimaryContainer,
    secondary = DaySecondary,
    onSecondary = DayOnSecondary,
    tertiary = DayTertiary,
    onTertiary = DayOnTertiary,
    error = DayError,
    onError = DayOnError,
    background = DayBackground,
    onBackground = DayTextPrimary,
    surface = DaySurface,
    onSurface = DayTextPrimary,
    surfaceVariant = DaySurfaceHigh,
    onSurfaceVariant = DayTextSecondary,
    outline = DayBorder
)

@Composable
fun SLHTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color turned off deliberately: coaching-center branding should stay
    // consistent across every phone instead of shifting with the user's wallpaper.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NightColorScheme else DayColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
