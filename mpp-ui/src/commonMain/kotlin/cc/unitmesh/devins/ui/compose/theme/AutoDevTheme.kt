package cc.unitmesh.devins.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF6750A4),
        secondary = Color(0xFF625B71),
        tertiary = Color(0xFF7D5260),
        background = Color(0xFF1C1B1F),
        surface = Color(0xFF1C1B1F),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFFFEFBFF),
        onSurface = Color(0xFFFEFBFF),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF6750A4),
        secondary = Color(0xFF625B71),
        tertiary = Color(0xFF7D5260),
        background = Color(0xFFFEFBFF),
        surface = Color(0xFFFEFBFF),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F),
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
