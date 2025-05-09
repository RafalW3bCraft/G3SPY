package com.g3spy.child.ui.theme

import androidx.compose.ui.graphics.Color

// Cyberpunk neon color palette - matching parent app for consistent design language
val NeonPink = Color(0xFFFF00FF)
val NeonBlue = Color(0xFF00FFFF)
val NeonGreen = Color(0xFF00FF00)
val NeonYellow = Color(0xFFFFFF00)
val NeonPurple = Color(0xFF9900FF)

// Dark backgrounds - slightly different hue for child app to distinguish it
val CyberDarkBg = Color(0xFF0A0A1E)  // Slightly bluer than parent app
val CyberDarkBgAlt = Color(0xFF151525)
val CyberDarkSurface = Color(0xFF101036)

// Text colors
val CyberTextPrimary = Color(0xFFFCFCFC)
val CyberTextSecondary = Color(0xFFADADAD)
val CyberTextAccent = NeonGreen  // Green accent to differentiate from parent app

// Accents
val CyberPrimaryLight = NeonGreen  // Primary color is green for child app
val CyberPrimaryDark = Color(0xFF007700)
val CyberSecondaryLight = NeonBlue
val CyberSecondaryDark = Color(0xFF006666)

// UI Elements
val CyberSuccess = NeonGreen
val CyberError = Color(0xFFFF0055)
val CyberWarning = NeonYellow
val CyberInfo = NeonBlue

// Gradient colors for backgrounds and buttons
val CyberGradientStart = Color(0xFF0A0A1E)
val CyberGradientMid = Color(0xFF101036)
val CyberGradientEnd = Color(0xFF151525)

// Legacy colors - keeping for compatibility
val Purple80 = NeonPurple
val PurpleGrey80 = CyberTextSecondary
val Pink80 = NeonPink

val Purple40 = CyberPrimaryDark
val PurpleGrey40 = CyberDarkSurface
val Pink40 = Color(0xFF9C0078)