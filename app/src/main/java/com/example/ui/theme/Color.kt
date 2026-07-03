package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BackgroundLight: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF111411) else Color(0xFFFBFDF7)

val TextPrimary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFE2E3DE) else Color(0xFF191C19)

val TextSecondary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFC1C9BE) else Color(0xFF424940)

val SurfaceVariant: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF424940) else Color(0xFFDDE5D9)

val HeroCardBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF84ACA3) else Color(0xFF386B3F)

val HeroTextLight: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF003314) else Color(0xFFB9CCB1)


val HeroButtonBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFD7E8CD) else Color(0xFFD7E8CD)

val HeroButtonText: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF002106) else Color(0xFF002106)

val AlertCardBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF93000A) else Color(0xFFFFDAD6)

val AlertIconBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFFB4AB) else Color(0xFF93000A)

val AlertTextPrimary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFFDAD6) else Color(0xFF410002)

val AlertTextSecondary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFFB4AB) else Color(0xFF93000A)

val GardenCardBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF004A77) else Color(0xFFD1E4FF)

val GardenTextPrimary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFD1E4FF) else Color(0xFF001D36)

val GardenTextSecondary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF9ECAFF) else Color(0xFF004A77)

val EnvCardBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF4F4539) else Color(0xFFE7E0D1)

val EnvTextPrimary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFE7E0D1) else Color(0xFF4F4539)

val EnvPillBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF6A5F51) else Color(0xFFD3C4AD)

val NavBarBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF191C19) else Color(0xFFF3F4ED)

val GreenPrimary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF84ACA3) else Color(0xFF6B9071)

val GreenLight: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF2C3E30) else Color(0xFFE8EFE9)

val TextDarkGreen: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFE8EFE9) else Color(0xFF2C3E30)

val AlertRed: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFFB4AB) else Color(0xFFE57373)

val AlertRedLight: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF93000A) else Color(0xFFFFEBEE)

