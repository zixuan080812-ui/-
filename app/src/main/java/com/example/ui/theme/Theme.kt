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
    primary = BubbleOutgoingDark,
    secondary = GlassSecondary,
    tertiary = Color(0xFFD0BCFF),
    background = Color.Transparent, // Let global gradient brush handle this
    surface = Color(0x2BFFFFFF),   // Translucent glass base (white/17%)
    onPrimary = Color.White,
    onSecondary = FrostedLightText,
    onBackground = FrostedLightText,
    onSurface = FrostedLightText,
    surfaceVariant = Color(0x40FFFFFF), // Slightly more opaque glass
    onSurfaceVariant = Color(0xFFCAC4D0)
)

private val LightColorScheme = lightColorScheme(
    primary = GlassPrimary,
    secondary = Color(0xFF4F46E5),
    tertiary = GlassTertiary,
    background = Color.Transparent, // Let global gradient brush handle this
    surface = Color(0x66FFFFFF),   // Translucent white glass (white/40%)
    onPrimary = Color.White,
    onSecondary = FrostedDarkText,
    onBackground = FrostedDarkText,
    onSurface = FrostedDarkText,
    surfaceVariant = Color(0x99FFFFFF), // White/60% glass helper
    onSurfaceVariant = Color(0xFF49454F) // M3 deep descriptive grey
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Always false so our gorgeous Custom Frosted Glass Palette shines!
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
