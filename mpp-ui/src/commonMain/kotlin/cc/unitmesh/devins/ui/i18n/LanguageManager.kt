package cc.unitmesh.devins.ui.i18n

import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.config.getLanguage
import cc.unitmesh.devins.ui.config.saveLanguagePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages language preferences and persistence
 */
object LanguageManager {
    private val _currentLanguage = MutableStateFlow(Language.ENGLISH)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    private var initialized = false

    /**
     * Initialize language from user preferences
     */
    suspend fun init() {
        if (initialized) return

        try {
            val config = ConfigManager.load()
            val langCode = config.getLanguage() ?: detectSystemLanguage()
            val language = Language.fromCode(langCode)
            setLanguage(language)
            initialized = true
        } catch (e: Exception) {
            // Use system language as fallback
            val language = Language.fromCode(detectSystemLanguage())
            setLanguage(language)
            initialized = true
        }
    }

    /**
     * Set current language and persist preference
     */
    suspend fun setLanguage(language: Language) {
        _currentLanguage.value = language
        Strings.setLanguage(language)

        // Persist to config
        try {
            saveLanguagePreference(language.code)
        } catch (e: Exception) {
            println("Failed to save language preference: ${e.message}")
        }
    }

    /**
     * Detect system language
     * Platform-specific implementations should override this
     */
    private fun detectSystemLanguage(): String {
        // Default to English
        // Platform-specific implementations will override this via expect/actual
        return "en"
    }

    /**
     * Get current language
     */
    fun getLanguage(): Language = _currentLanguage.value
}
