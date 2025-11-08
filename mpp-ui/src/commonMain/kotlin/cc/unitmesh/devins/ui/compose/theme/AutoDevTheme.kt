package cc.unitmesh.devins.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * AutoDev 暗色主题配色方案
 * 使用新的设计系统颜色
 */
private val DarkColorScheme =
    darkColorScheme(
        // 主色 - Intelligent Indigo
        primary = AutoDevColors.Indigo.c300,
        onPrimary = AutoDevColors.Neutral.c900,
        primaryContainer = AutoDevColors.Indigo.c800,
        onPrimaryContainer = AutoDevColors.Indigo.c100,
        // 辅色 - Spark Cyan
        secondary = AutoDevColors.Cyan.c400,
        onSecondary = AutoDevColors.Neutral.c900,
        secondaryContainer = AutoDevColors.Cyan.c800,
        onSecondaryContainer = AutoDevColors.Cyan.c100,
        // 第三色
        tertiary = AutoDevColors.Green.c300,
        onTertiary = AutoDevColors.Neutral.c900,
        tertiaryContainer = AutoDevColors.Green.c800,
        onTertiaryContainer = AutoDevColors.Green.c100,
        // 背景和表面
        background = AutoDevColors.Neutral.c900,
        onBackground = AutoDevColors.Neutral.c100,
        surface = AutoDevColors.Neutral.c800,
        onSurface = AutoDevColors.Neutral.c100,
        surfaceVariant = AutoDevColors.Neutral.c700,
        onSurfaceVariant = AutoDevColors.Neutral.c300,
        // 错误
        error = AutoDevColors.Red.c300,
        onError = AutoDevColors.Neutral.c900,
        errorContainer = AutoDevColors.Red.c900,
        onErrorContainer = AutoDevColors.Red.c100,
        // 轮廓
        outline = AutoDevColors.Neutral.c700,
        outlineVariant = AutoDevColors.Neutral.c800,
    )

/**
 * AutoDev 亮色主题配色方案
 * 使用新的设计系统颜色
 */
private val LightColorScheme =
    lightColorScheme(
        // 主色 - Intelligent Indigo
        primary = AutoDevColors.Indigo.c600,
        onPrimary = Color.White,
        primaryContainer = AutoDevColors.Indigo.c100,
        onPrimaryContainer = AutoDevColors.Indigo.c900,
        // 辅色 - Spark Cyan
        secondary = AutoDevColors.Cyan.c500,
        onSecondary = Color.White,
        secondaryContainer = AutoDevColors.Cyan.c100,
        onSecondaryContainer = AutoDevColors.Cyan.c900,
        // 第三色
        tertiary = AutoDevColors.Green.c600,
        onTertiary = Color.White,
        tertiaryContainer = AutoDevColors.Green.c100,
        onTertiaryContainer = AutoDevColors.Green.c900,
        // 背景和表面
        background = AutoDevColors.Neutral.c50,
        onBackground = AutoDevColors.Neutral.c900,
        surface = Color.White,
        onSurface = AutoDevColors.Neutral.c900,
        surfaceVariant = AutoDevColors.Neutral.c100,
        onSurfaceVariant = AutoDevColors.Neutral.c700,
        // 错误
        error = AutoDevColors.Red.c600,
        onError = Color.White,
        errorContainer = AutoDevColors.Red.c100,
        onErrorContainer = AutoDevColors.Red.c900,
        // 轮廓
        outline = AutoDevColors.Neutral.c300,
        outlineVariant = AutoDevColors.Neutral.c200,
    )

/**
 * AutoDev 主题
 * 支持白天模式、夜间模式和跟随系统
 */
@Composable
fun AutoDevTheme(
    themeMode: ThemeManager.ThemeMode = ThemeManager.currentTheme,
    content: @Composable () -> Unit
) {
    val systemInDarkTheme = isSystemInDarkTheme()

    // 根据主题模式决定是否使用深色主题
    val darkTheme =
        when (themeMode) {
            ThemeManager.ThemeMode.LIGHT -> false
            ThemeManager.ThemeMode.DARK -> true
            ThemeManager.ThemeMode.SYSTEM -> systemInDarkTheme
        }

    val colorScheme =
        if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

/**
 * 向后兼容的旧版 API
 */
@Composable
fun AutoDevTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val themeMode =
        if (darkTheme) {
            ThemeManager.ThemeMode.DARK
        } else {
            ThemeManager.ThemeMode.LIGHT
        }

    AutoDevTheme(themeMode = themeMode, content = content)
}
