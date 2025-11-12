package cc.unitmesh.devins.ui.compose.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.devins.ui.i18n.Strings

/**
 * Theme Manager
 * Manages the application's theme mode (Light/Dark/System)
 */
object ThemeManager {
    /**
     * Theme mode
     */
    enum class ThemeMode {
        LIGHT, // Light mode
        DARK, // Dark mode
        SYSTEM // Follow system
    }

    /**
     * Current theme mode
     */
    var currentTheme by mutableStateOf(ThemeMode.SYSTEM)
        private set

    /**
     * Set theme
     */
    fun setTheme(mode: ThemeMode) {
        currentTheme = mode
        // TODO: Add persistence logic to save to config file
        println(Strings.themeSwitched(getThemeDisplayName(mode)))
    }

    /**
     * Toggle to next theme
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
     * Get theme display name
     */
    fun getThemeDisplayName(mode: ThemeMode): String {
        return when (mode) {
            ThemeMode.LIGHT -> Strings.themeLight
            ThemeMode.DARK -> Strings.themeDark
            ThemeMode.SYSTEM -> Strings.themeSystem
        }
    }

    /**
     * Get current theme display name
     */
    fun getCurrentThemeDisplayName(): String {
        return getThemeDisplayName(currentTheme)
    }
}
