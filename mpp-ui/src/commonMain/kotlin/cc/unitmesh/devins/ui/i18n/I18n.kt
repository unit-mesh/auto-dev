package cc.unitmesh.devins.ui.i18n

import de.comahe.i18n4k.Locale
import de.comahe.i18n4k.config.I18n4kConfigDefault
import de.comahe.i18n4k.forLocaleTag
import de.comahe.i18n4k.i18n4k

/**
 * Internationalization support for AutoDev UI
 *
 * Uses i18n4k library for multiplatform i18n support
 * Supports English and Chinese (Simplified)
 */

/**
 * Supported languages
 */
enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    CHINESE("zh", "中文");

    companion object {
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }

    /**
     * Convert to i18n4k Locale
     */
    fun toLocale(): Locale = forLocaleTag(code)
}

/**
 * Translation keys and values using i18n4k
 */
object Strings {
    private var currentLanguage = Language.ENGLISH
    private val i18n4kConfig = I18n4kConfigDefault()

    init {
        // Initialize i18n4k with default config
        i18n4k = i18n4kConfig
    }

    /**
     * Set current language
     */
    fun setLanguage(lang: Language) {
        currentLanguage = lang
        // Update i18n4k locale
        i18n4kConfig.locale = lang.toLocale()
    }

    /**
     * Get current language
     */
    fun getLanguage(): Language = currentLanguage

    // Common strings
    val save: String get() = AutoDevStrings.common_save.toString()
    val cancel: String get() = AutoDevStrings.common_cancel.toString()
    val back: String get() = AutoDevStrings.common_back.toString()
    val confirm: String get() = AutoDevStrings.common_confirm.toString()
    val configure: String get() = AutoDevStrings.common_configure.toString()
    val loading: String get() = AutoDevStrings.common_loading.toString()

    // Chat UI
    val chatTitle: String get() = AutoDevStrings.chat_title.toString()
    val openDirectory: String get() = AutoDevStrings.chat_openDirectory.toString()
    val openProject: String get() = AutoDevStrings.chat_openProject.toString()
    val newChat: String get() = AutoDevStrings.chat_newChat.toString()
    val debugInfo: String get() = AutoDevStrings.chat_debugInfo.toString()

    // Model Configuration
    val modelConfigTitle: String get() = AutoDevStrings.modelConfig_title.toString()
    val provider: String get() = AutoDevStrings.modelConfig_provider.toString()
    val model: String get() = AutoDevStrings.modelConfig_model.toString()
    val modelHint: String get() = AutoDevStrings.modelConfig_modelHint.toString()
    val apiKey: String get() = AutoDevStrings.modelConfig_apiKey.toString()
    val baseUrl: String get() = AutoDevStrings.modelConfig_baseUrl.toString()
    val temperature: String get() = AutoDevStrings.modelConfig_temperature.toString()
    val maxTokens: String get() = AutoDevStrings.modelConfig_maxTokens.toString()
    val advancedParameters: String get() = AutoDevStrings.modelConfig_advancedParameters.toString()
    val configureModel: String get() = AutoDevStrings.modelConfig_configureModel.toString()
    val noSavedConfigs: String get() = AutoDevStrings.modelConfig_noSavedConfigs.toString()
    val enterModel: String get() = AutoDevStrings.modelConfig_enterModel.toString()
    val enterApiKey: String get() = AutoDevStrings.modelConfig_enterApiKey.toString()
    val showApiKey: String get() = AutoDevStrings.modelConfig_showApiKey.toString()
    val hideApiKey: String get() = AutoDevStrings.modelConfig_hideApiKey.toString()
    val maxResponseLength: String get() = AutoDevStrings.modelConfig_maxResponseLength.toString()
    val temperatureRange: String get() = AutoDevStrings.modelConfig_temperatureRange.toString()
    val selected: String get() = AutoDevStrings.modelConfig_selected.toString()

    // Messages with parameters
    fun failedToLoadConfigs(error: String): String = AutoDevStrings.messages_failedToLoadConfigs(error).toString()

    fun failedToSetActiveConfig(error: String): String = AutoDevStrings.messages_failedToSetActiveConfig(error).toString()

    fun failedToSaveConfig(error: String): String = AutoDevStrings.messages_failedToSaveConfig(error).toString()
}

