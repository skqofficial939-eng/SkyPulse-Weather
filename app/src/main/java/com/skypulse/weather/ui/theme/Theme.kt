package com.skypulse.weather.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skypulse.weather.domain.ThemeMode
import com.skypulse.weather.domain.WeatherKind

object Sky {
    val Midnight = Color(0xFF071426)
    val DeepBlue = Color(0xFF0B1F3A)
    val Electric = Color(0xFF1677FF)
    val Cyan = Color(0xFF42D9FF)
    val Cloud = Color(0xFFF5F8FF)
    val Sun = Color(0xFFFFC53D)
    val MutedOnDark = Color(0xFF9BB0CC)
    val MutedOnLight = Color(0xFF4F627D)
    val CardDark = Color(0xFF102848)
    val CardLight = Color(0xFFFFFFFF)
    val Warning = Color(0xFFFFB020)
}

private val DarkScheme = darkColorScheme(
    primary = Sky.Cyan, onPrimary = Sky.Midnight,
    secondary = Sky.Electric, onSecondary = Color.White,
    background = Sky.Midnight, onBackground = Sky.Cloud,
    surface = Sky.DeepBlue, onSurface = Sky.Cloud,
    surfaceVariant = Sky.CardDark, onSurfaceVariant = Sky.MutedOnDark,
    outline = Color(0xFF2B4468),
    error = Color(0xFFFF8A80),
)

private val LightScheme = lightColorScheme(
    primary = Sky.Electric, onPrimary = Color.White,
    secondary = Color(0xFF0A5BD6), onSecondary = Color.White,
    background = Sky.Cloud, onBackground = Sky.Midnight,
    surface = Color.White, onSurface = Sky.Midnight,
    surfaceVariant = Sky.CardLight, onSurfaceVariant = Sky.MutedOnLight,
    outline = Color(0xFFC5D2E6),
    error = Color(0xFFB3261E),
)

private val SkyTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 96.sp, lineHeight = 100.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

private val SkyShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

@Composable
fun resolveDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun SkyPulseTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = resolveDark(mode)
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = SkyTypography, shapes = SkyShapes, content = content)
}

/** Hero gradients are intentionally mid/dark so white text stays readable in both app themes. */
fun skyBrush(kind: WeatherKind, isDay: Boolean): Brush {
    val colors = if (!isDay) listOf(Color(0xFF071A38), Color(0xFF123566)) else when (kind) {
        WeatherKind.CLEAR -> listOf(Color(0xFF0D55B8), Color(0xFF2A86DB))
        WeatherKind.PARTLY_CLOUDY -> listOf(Color(0xFF1B5AA8), Color(0xFF4A86C4))
        WeatherKind.CLOUDY -> listOf(Color(0xFF3B5272), Color(0xFF5C7596))
        WeatherKind.FOG -> listOf(Color(0xFF4C5868), Color(0xFF6E7B8C))
        WeatherKind.DRIZZLE, WeatherKind.RAIN -> listOf(Color(0xFF2A3D5C), Color(0xFF4A6280))
        WeatherKind.SNOW -> listOf(Color(0xFF42628A), Color(0xFF6C8DB3))
        WeatherKind.STORM -> listOf(Color(0xFF241F45), Color(0xFF45407A))
        WeatherKind.UNKNOWN -> listOf(Sky.DeepBlue, Color(0xFF1B3F72))
    }
    return Brush.verticalGradient(colors)
}
