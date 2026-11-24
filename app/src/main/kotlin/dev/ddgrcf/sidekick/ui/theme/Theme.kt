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
    val cardBorder: Color,
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
    val brandGradientStart: Color,
    val brandGradientEnd: Color,
)

private val DarkExtendedColors = SidekickExtendedColors(
    isDark = true,
    cardBorder = Color(0xFF243046),
    success = Color(0xFF22C55E),
    successContainer = Color(0xFF102A1E),
    onSuccessContainer = Color(0xFF86EFAC),
    warning = Color(0xFFF59E0B),
    warningContainer = Color(0xFF2D210B),
    onWarningContainer = Color(0xFFFDE68A),
    userBubble = Color(0xFF162C4E),
    onUserBubble = Color(0xFFEFF6FF),
    userBubbleBorder = Color(0xFF274D86),
    codeBg = Color(0xFF0A0D13),
    codeHeaderBg = Color(0xFF131924),
    codeBorder = Color(0xFF222D40),
    codeText = Color(0xFFDDE6F5),
    terminalBg = Color(0xFF0B0E14),
    terminalFg = Color(0xFFE2E8F0),
    terminalBorder = Color(0xFF243046),
    keycapBg = Color(0xFF182030),
    keycapBorder = Color(0xFF2B384F),
    keycapText = Color(0xFFD6E0F0),
    keycapDangerBg = Color(0xFF36181F),
    keycapDangerText = Color(0xFFFCA5A5),
    brandGradientStart = Color(0xFF162A4C),
    brandGradientEnd = Color(0xFF0E2930),
)

private val LightExtendedColors = SidekickExtendedColors(
    isDark = false,
    cardBorder = Color(0xFFDDE5F0),
    success = Color(0xFF16A34A),
    successContainer = Color(0xFFDCFCE7),
    onSuccessContainer = Color(0xFF166534),
    warning = Color(0xFFD97706),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF92400E),
    userBubble = Color(0xFFDBEAFE),
    onUserBubble = Color(0xFF0F172A),
    userBubbleBorder = Color(0xFF93C5FD),
    codeBg = Color(0xFFF1F5F9),
    codeHeaderBg = Color(0xFFE2E8F0),
    codeBorder = Color(0xFFCBD5E1),
    codeText = Color(0xFF0F172A),
    terminalBg = Color(0xFF0B0E14),
    terminalFg = Color(0xFFE2E8F0),
    terminalBorder = Color(0xFFCBD5E1),
    keycapBg = Color(0xFFF1F5F9),
    keycapBorder = Color(0xFFCBD5E1),
    keycapText = Color(0xFF1E293B),
    keycapDangerBg = Color(0xFFFEE2E2),
    keycapDangerText = Color(0xFFB91C1C),
    brandGradientStart = Color(0xFFDBEAFE),
    brandGradientEnd = Color(0xFFCCFBF1),
)

val LocalSidekickColors = staticCompositionLocalOf { DarkExtendedColors }

object SidekickThemeExtras {
    val colors: SidekickExtendedColors
        @Composable get() = LocalSidekickColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF06162B),
    primaryContainer = Color(0xFF172B4D),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF133A39),
    onSecondaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFE6EDF7),
    surface = Color(0xFF10141D),
    onSurface = Color(0xFFE6EDF7),
    surfaceVariant = Color(0xFF1A2233),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF080A0F),
    surfaceContainerLow = Color(0xFF141925),
    surfaceContainer = Color(0xFF181F2E),
    surfaceContainerHigh = Color(0xFF1E2738),
    surfaceContainerHighest = Color(0xFF263147),
    outline = Color(0xFF3B4A66),
    outlineVariant = Color(0xFF243046),
    error = Color(0xFFF87171),
    onError = Color(0xFF3B0909),
    errorContainer = Color(0xFF3D151B),
    onErrorContainer = Color(0xFFFECACA),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEDF2F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5F9),
    surfaceContainer = Color(0xFFEBF0F7),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFD8E1EC),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
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
