package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF00363F),
    primaryContainer = CyanPrimaryContainer,
    onPrimaryContainer = OnCyanPrimaryContainer,
    secondary = VioletSecondary,
    onSecondary = Color.White,
    secondaryContainer = VioletSecondaryContainer,
    onSecondaryContainer = OnVioletSecondaryContainer,
    tertiary = PinkTertiary,
    onTertiary = Color.White,
    tertiaryContainer = PinkTertiaryContainer,
    background = CosmicBackground,
    onBackground = TextPrimaryDark,
    surface = CosmicSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = CosmicBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007A8C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC3F3FA),
    onPrimaryContainer = Color(0xFF002026),
    secondary = Color(0xFF7B2CBF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E6FF),
    onSecondaryContainer = Color(0xFF2C004F),
    tertiary = Color(0xFFD81E5B),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder
)

@Composable
fun ChottuAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinctive futuristic Chottu AI theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
