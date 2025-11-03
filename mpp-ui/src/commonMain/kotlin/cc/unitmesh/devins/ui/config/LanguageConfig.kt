package cc.unitmesh.devins.ui.config

/**
 * Language configuration extension for ConfigFile
 */

/**
 * Get language from config file
 * Returns null if language not set (will fall back to system language)
 */
fun AutoDevConfigWrapper.getLanguage(): String? {
    // For now, return null to use system language detection
    // TODO: Add language field to ConfigFile when needed
    return null
}

/**
 * Save language preference to config file
 *
 * Note: This is a placeholder implementation.
 * In a real implementation, we would add a 'language' field to ConfigFile.
 */
suspend fun saveLanguagePreference(languageCode: String) {
    try {
        // TODO: Properly implement language persistence
        // For now, just log the preference
        println("Language preference set to: $languageCode")
    } catch (e: Exception) {
        println("Failed to save language preference: ${e.message}")
    }
}
