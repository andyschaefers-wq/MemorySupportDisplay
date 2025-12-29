package com.otheruncle.memorydisplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Primary colors - Green family (good for elderly perception)
val PrimaryGreen = Color(0xFF2E7D32)
val PrimaryGreenDark = Color(0xFF1B5E20)
val PrimaryGreenLight = Color(0xFF4CAF50)

// Secondary colors
val SecondaryGreen = Color(0xFF558B2F)
val SecondaryGreenDark = Color(0xFF33691E)

// Background colors
val BackgroundLight = Color(0xFFFAFAFA)
val SurfaceLight = Color(0xFFFFFFFF)

// Temporal badge colors (matching display)
val BadgeToday = Color(0xFFFF9800)
val BadgeTomorrow = Color(0xFFFFC107)
val BadgeUpcoming = Color(0xFF4CAF50)

// Card type background tints
val CardProfessional = Color(0xFFE3F2FD)
val CardFamilyEvent = Color(0xFFFFF3E0)
val CardOtherEvent = Color(0xFFF3E5F5)
val CardTrip = Color(0xFFE8F5E9)
val CardReminder = Color(0xFFFFF8E1)
val CardMessage = Color(0xFFFCE4EC)
val CardPending = Color(0xFFECEFF1)

// Error
val ErrorRed = Color(0xFFB00020)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = PrimaryGreenLight,
    onPrimaryContainer = PrimaryGreenDark,
    secondary = SecondaryGreen,
    onSecondary = Color.White,
    secondaryContainer = SecondaryGreenDark,
    onSecondaryContainer = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF212121),
    surface = SurfaceLight,
    onSurface = Color(0xFF212121),
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreenLight,
    onPrimary = PrimaryGreenDark,
    primaryContainer = PrimaryGreen,
    onPrimaryContainer = Color.White,
    secondary = SecondaryGreen,
    onSecondary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun MemorySupportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
