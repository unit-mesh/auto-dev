package cc.unitmesh.devins.ui.config

/**
 * Language configuration extension for ConfigFile
 */

/**
 * Get language from config file
 * Returns the saved language preference or null if not set (will fall back to system language)
 */
fun AutoDevConfigWrapper.getLanguage(): String? {
    return configFile.language.takeIf { it.isNotEmpty() }
}

/**
 * Save language preference to config file
 * 
 * This updates the language field in the config file and persists it.
 */
suspend fun saveLanguagePreference(languageCode: String) {
    try {
        val currentConfig = ConfigManager.load()
        val updatedConfig = currentConfig.configFile.copy(language = languageCode)
        ConfigManager.save(updatedConfig)
        println("Language preference saved: $languageCode")
    } catch (e: Exception) {
        println("Failed to save language preference: ${e.message}")
        throw e
    }
}
