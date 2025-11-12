package cc.unitmesh.devins.ui.config

/**
 * Language configuration extension for ConfigFile
 */

/**
 * Get language from config file
 * Returns the saved language preference or null if not set (will fall back to system language)
 */
fun AutoDevConfigWrapper.getLanguage(): String? {
    return configFile.language.takeIf { it?.isNotEmpty() == true }
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

/**
 * Save agent type preference to config file
 *
 * This updates the agentType field in the config file and persists it.
 */
suspend fun saveAgentTypePreference(agentType: String) {
    try {
        val currentConfig = ConfigManager.load()
        val updatedConfig = currentConfig.configFile.copy(agentType = agentType)
        ConfigManager.save(updatedConfig)
        println("Agent type preference saved: $agentType")
    } catch (e: Exception) {
        println("Failed to save agent type preference: ${e.message}")
        throw e
    }
}
