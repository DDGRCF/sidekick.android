package dev.ddgrcf.sidekick.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "跟随系统"),
    DARK("dark", "深色"),
    LIGHT("light", "浅色"),
    ;

    companion object {
        fun fromKey(key: String): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

@Immutable
data class SidekickExtendedColors(
    val isDark: Boolean,
    val hairline: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val userBubble: Color,
    val onUserBubble: Color,
    val userBubbleBorder: Color,
    val codeBg: Color,
    val codeHeaderBg: Color,
    val codeBorder: Color,
    val codeText: Color,
    val terminalBg: Color,
    val terminalFg: Color,
    val terminalBorder: Color,
    val keycapBg: Color,
    val keycapBorder: Color,
    val keycapText: Color,
    val keycapDangerBg: Color,
    val keycapDangerText: Color,
)

private val DarkExtendedColors = SidekickExtendedColors(
    isDark = true,
    hairline = Color(0xFF252A36),
    success = Color(0xFF34D399),
    successContainer = Color(0xFF0E2920),
    onSuccessContainer = Color(0xFFA7F3D0),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF2C200A),
    onWarningContainer = Color(0xFFFDE68A),
    userBubble = Color(0xFF1B1F28),
    onUserBubble = Color(0xFFE6EDF3),
    userBubbleBorder = Color(0xFF2A3140),
    codeBg = Color(0xFF0D1117),
    codeHeaderBg = Color(0xFF151A22),
    codeBorder = Color(0xFF252A36),
    codeText = Color(0xFFE6EDF3),
    terminalBg = Color(0xFF0B0E14),
    terminalFg = Color(0xFFE6EDF3),
    terminalBorder = Color(0xFF252A36),
    keycapBg = Color(0xFF171C26),
    keycapBorder = Color(0xFF283040),
    keycapText = Color(0xFFD6E0F0),
    keycapDangerBg = Color(0xFF35171D),
    keycapDangerText = Color(0xFFFCA5A5),
)

private val LightExtendedColors = SidekickExtendedColors(
    isDark = false,
    hairline = Color(0xFFE2E6EC),
    success = Color(0xFF047857),
    successContainer = Color(0xFFD1FAE5),
    onSuccessContainer = Color(0xFF065F46),
    warning = Color(0xFFB45309),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF92400E),
    userBubble = Color(0xFFF6F7F9),
    onUserBubble = Color(0xFF0F172A),
    userBubbleBorder = Color(0xFFE2E6EC),
    codeBg = Color(0xFFF6F7F9),
    codeHeaderBg = Color(0xFFECEFF4),
    codeBorder = Color(0xFFE2E6EC),
    codeText = Color(0xFF1F2328),
    terminalBg = Color(0xFF0B0E14),
    terminalFg = Color(0xFFE6EDF3),
    terminalBorder = Color(0xFFCBD5E1),
    keycapBg = Color(0xFFF6F7F9),
    keycapBorder = Color(0xFFD5DCE6),
    keycapText = Color(0xFF1E293B),
    keycapDangerBg = Color(0xFFFEE2E2),
    keycapDangerText = Color(0xFFB91C1C),
)

val LocalSidekickColors = staticCompositionLocalOf { DarkExtendedColors }

object SidekickThemeExtras {
    val colors: SidekickExtendedColors
        @Composable get() = LocalSidekickColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF042218),
    primaryContainer = Color(0xFF0F2D23),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF06162B),
    secondaryContainer = Color(0xFF15253E),
    onSecondaryContainer = Color(0xFFDBEAFE),
    background = Color(0xFF0C0E12),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF13161D),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1B1F28),
    onSurfaceVariant = Color(0xFF8B949E),
    surfaceContainerLowest = Color(0xFF090B0E),
    surfaceContainerLow = Color(0xFF13161D),
    surfaceContainer = Color(0xFF171B24),
    surfaceContainerHigh = Color(0xFF1B1F28),
    surfaceContainerHighest = Color(0xFF232834),
    outline = Color(0xFF3A4254),
    outlineVariant = Color(0xFF252A36),
    error = Color(0xFFF87171),
    onError = Color(0xFF3B0909),
    errorContainer = Color(0xFF3D151B),
    onErrorContainer = Color(0xFFFECACA),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF2563EB),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF6F7F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F7F9),
    surfaceContainer = Color(0xFFEEF1F5),
    surfaceContainerHigh = Color(0xFFE6EAF0),
    surfaceContainerHighest = Color(0xFFDCE1EA),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E6EC),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
)

private val SidekickTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

private val SidekickShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun SidekickTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalSidekickColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SidekickTypography,
            shapes = SidekickShapes,
            content = content,
        )
    }
}
