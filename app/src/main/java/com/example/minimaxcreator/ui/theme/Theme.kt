package com.example.minimaxcreator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0071E3),
    onPrimary = Color.White,
    primaryContainer = Color(0xCCD9EFFF),
    onPrimaryContainer = Color(0xFF00365A),
    secondary = Color(0xFF1D1D1F),
    secondaryContainer = Color(0x88E9E9ED),
    onSecondaryContainer = Color(0xFF1D1D1F),
    background = Color.Transparent,
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xCCFFFFFF),
    onSurface = Color(0xFF1D1D1F),
    surfaceVariant = Color(0x88ECECEF),
    onSurfaceVariant = Color(0xFF6E6E73),
    error = Color(0xFFB3261E),
    outline = Color(0x55FFFFFF),
    outlineVariant = Color(0x55FFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color(0xFF002B4A),
    primaryContainer = Color(0xCC004B75),
    onPrimaryContainer = Color(0xFFD9EFFF),
    secondary = Color(0xFFF5F5F7),
    secondaryContainer = Color(0x99555558),
    onSecondaryContainer = Color(0xFFF5F5F7),
    background = Color.Transparent,
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xCC1A1A1C),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0x66555558),
    onSurfaceVariant = Color(0xFFC7C7CC),
    error = Color(0xFFFF6961),
    outline = Color(0x44FFFFFF),
    outlineVariant = Color(0x33FFFFFF)
)

@Composable
fun StudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> {
            androidx.compose.material3.dynamicDarkColorScheme(
                androidx.compose.ui.platform.LocalContext.current
            )
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            androidx.compose.material3.dynamicLightColorScheme(
                androidx.compose.ui.platform.LocalContext.current
            )
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
