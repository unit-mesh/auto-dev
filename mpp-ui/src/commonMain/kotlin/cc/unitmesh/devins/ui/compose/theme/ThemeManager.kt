package cc.unitmesh.devins.ui.compose.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 主题管理器
 * 管理应用的主题模式（白天/夜间）
 */
object ThemeManager {
    /**
     * 主题模式
     */
    enum class ThemeMode {
        LIGHT, // 白天模式
        DARK, // 夜间模式
        SYSTEM // 跟随系统
    }

    /**
     * 当前主题模式
     */
    var currentTheme by mutableStateOf(ThemeMode.SYSTEM)
        private set

    /**
     * 切换主题
     */
    fun setTheme(mode: ThemeMode) {
        currentTheme = mode
        // 这里可以添加持久化逻辑，保存到配置文件
        println("🎨 切换主题: $mode")
    }

    /**
     * 切换到下一个主题
     */
    fun toggleTheme() {
        currentTheme =
            when (currentTheme) {
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
            }
    }

    /**
     * 获取主题显示名称
     */
    fun getThemeDisplayName(mode: ThemeMode): String {
        return when (mode) {
            ThemeMode.LIGHT -> "☀️ 白天模式"
            ThemeMode.DARK -> "🌙 夜间模式"
            ThemeMode.SYSTEM -> "🖥️ 跟随系统"
        }
    }

    /**
     * 获取当前主题的显示名称
     */
    fun getCurrentThemeDisplayName(): String {
        return getThemeDisplayName(currentTheme)
    }
}
