package com.slicehouse.pos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slicehouse.pos.data.ThemeMode

val Tomato = Color(0xFFB23A2E)
val TomatoDark = Color(0xFF7E1F17)
val Cream = Color(0xFFFFF8F4)
val Ink = Color(0xFF241A17)
val Basil = Color(0xFF3F6B35)
val Mustard = Color(0xFF8A6500)

private val LightColors = lightColorScheme(
    primary = Tomato,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD3),
    onPrimaryContainer = Color(0xFF410E06),
    secondary = Mustard,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF261A00),
    tertiary = Basil,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC6F0B4),
    onTertiaryContainer = Color(0xFF082100),
    background = Cream,
    onBackground = Ink,
    surface = Color(0xFFFFFBF8),
    onSurface = Ink,
    surfaceVariant = Color(0xFFF4DED8),
    onSurfaceVariant = Color(0xFF584440),
    outline = Color(0xFF8A7570),
    outlineVariant = Color(0xFFDCC5BF),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceDim = Color(0xFFE9D6D0),
    surfaceBright = Color(0xFFFFFBF8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF2ED),
    surfaceContainer = Color(0xFFFBEBE6),
    surfaceContainerHigh = Color(0xFFF6E4DF),
    surfaceContainerHighest = Color(0xFFF0DED8)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A8),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF8C1D12),
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFFFFD54F),
    onSecondary = Color(0xFF3A2E00),
    secondaryContainer = Color(0xFF574400),
    onSecondaryContainer = Color(0xFFFFE08A),
    tertiary = Color(0xFFA8D68F),
    onTertiary = Color(0xFF123800),
    tertiaryContainer = Color(0xFF2A5217),
    onTertiaryContainer = Color(0xFFC6F0B4),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF2DEDA),
    surface = Color(0xFF211715),
    onSurface = Color(0xFFF2DEDA),
    surfaceVariant = Color(0xFF534340),
    onSurfaceVariant = Color(0xFFD8C2BD),
    outline = Color(0xFFA08C87),
    outlineVariant = Color(0xFF534340),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceDim = Color(0xFF1A1110),
    surfaceBright = Color(0xFF423735),
    surfaceContainerLowest = Color(0xFF140C0B),
    surfaceContainerLow = Color(0xFF221816),
    surfaceContainer = Color(0xFF271C1A),
    surfaceContainerHigh = Color(0xFF322624),
    surfaceContainerHighest = Color(0xFF3D312E)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun SliceHouseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isDarkTheme(themeMode)) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
