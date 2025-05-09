package com.g3spy.parent.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Cyberpunk Dark Theme
private val CyberpunkDarkScheme = darkColorScheme(
    primary = NeonBlue,
    onPrimary = Color.Black,
    primaryContainer = CyberPrimaryDark,
    onPrimaryContainer = CyberTextPrimary,
    
    secondary = NeonPink,
    onSecondary = Color.Black,
    secondaryContainer = CyberSecondaryDark,
    onSecondaryContainer = CyberTextPrimary,
    
    tertiary = NeonGreen,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF003300),
    onTertiaryContainer = CyberTextPrimary,
    
    background = CyberDarkBg,
    onBackground = CyberTextPrimary,
    
    surface = CyberDarkSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberDarkBgAlt,
    onSurfaceVariant = CyberTextSecondary,
    
    error = CyberError,
    onError = Color.Black,
    
    outline = NeonBlue,
    outlineVariant = CyberPrimaryDark
)

// Cyberpunk Light Theme - still use dark colors but with higher contrast for better usability
private val CyberpunkLightScheme = darkColorScheme(
    primary = NeonBlue,
    onPrimary = Color.Black,
    primaryContainer = CyberPrimaryLight.copy(alpha = 0.8f),
    onPrimaryContainer = Color.Black,
    
    secondary = NeonPink,
    onSecondary = Color.Black,
    secondaryContainer = CyberSecondaryLight.copy(alpha = 0.8f),
    onSecondaryContainer = Color.Black,
    
    tertiary = NeonYellow,
    onTertiary = Color.Black,
    tertiaryContainer = NeonYellow.copy(alpha = 0.5f),
    onTertiaryContainer = Color.Black,
    
    background = CyberDarkBgAlt.copy(alpha = 0.95f),
    onBackground = CyberTextPrimary,
    
    surface = CyberDarkSurface.copy(alpha = 0.9f),
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberDarkBg.copy(alpha = 0.8f),
    onSurfaceVariant = CyberTextPrimary,
    
    error = CyberError,
    onError = Color.Black,
    
    outline = NeonBlue,
    outlineVariant = CyberPrimaryLight
)

// Legacy color schemes for backward compatibility
private val DarkColorScheme = CyberpunkDarkScheme
private val LightColorScheme = CyberpunkLightScheme

@Composable
fun G3SPYTheme(
    darkTheme: Boolean = true, // Always default to dark theme for cyberpunk feel
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic colors to maintain cyberpunk aesthetic
    content: @Composable () -> Unit
) {
    // Force dark theme for cyberpunk feel, ignore system settings
    val colorScheme = CyberpunkDarkScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar to a dark color with a hint of neon
            window.statusBarColor = CyberDarkBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            
            // Navigation bar styling
            window.navigationBarColor = CyberDarkBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BaseTypography,
        content = content
    )
}
