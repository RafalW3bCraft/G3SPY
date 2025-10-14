package com.g3spy.child.ui.theme

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

private val CyberpunkDarkScheme = darkColorScheme(
    primary = NeonGreen,  
    onPrimary = Color.Black,
    primaryContainer = CyberPrimaryDark, 
    onPrimaryContainer = CyberTextPrimary,
    
    secondary = NeonBlue,
    onSecondary = Color.Black,
    secondaryContainer = CyberSecondaryDark,
    onSecondaryContainer = CyberTextPrimary,
    
    tertiary = NeonPink,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF330033),
    onTertiaryContainer = CyberTextPrimary,
    
    background = CyberDarkBg,
    onBackground = CyberTextPrimary,
    
    surface = CyberDarkSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberDarkBgAlt,
    onSurfaceVariant = CyberTextSecondary,
    
    error = CyberError,
    onError = Color.Black,
    
    outline = NeonGreen,
    outlineVariant = CyberPrimaryDark
)

private val CyberpunkLightScheme = CyberpunkDarkScheme

@Composable
fun G3SPYChildTheme(
    darkTheme: Boolean = true, 
    
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    
    val colorScheme = CyberpunkDarkScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            window.statusBarColor = CyberDarkBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            
            window.navigationBarColor = CyberDarkBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}