package com.hallzyx.detengue.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Colores de marca: verde azulado sereno, nada de estética de emergencia médica (PRD §48). */
private val Teal = Color(0xFF0F766E)
private val TealDark = Color(0xFF5EEAD4)

/** Colores semánticos de estado (PRD §32): se definen por significado, no por marca. */
object ConditionColors {
    val Safe = Color(0xFF2E7D32)
    val Review = Color(0xFFF59E0B)
    val Risk = Color(0xFFC62828)
    val Uncertain = Color(0xFF6B7280)
}

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF042F2E),
    background = Color(0xFFF7FAF9),
    onBackground = Color(0xFF10201E),
    surface = Color(0xFFF7FAF9),
    onSurface = Color(0xFF10201E),
    surfaceVariant = Color(0xFFE2ECEA),
    onSurfaceVariant = Color(0xFF3F4F4C),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = TealDark,
    onPrimary = Color(0xFF00201D),
    primaryContainer = Color(0xFF0B4F4A),
    onPrimaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFE2ECEA),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFE2ECEA),
    surfaceVariant = Color(0xFF26332F),
    onSurfaceVariant = Color(0xFFBFCDCA),
)

/** Textos grandes y alto contraste (PRD §47). */
private val DetengueTypography = Typography(
    headlineLarge = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun DetengueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DetengueTypography,
        content = content,
    )
}
