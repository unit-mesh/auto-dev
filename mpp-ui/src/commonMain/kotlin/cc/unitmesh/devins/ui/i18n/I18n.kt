package cc.unitmesh.devins.ui.i18n

/**
 * Internationalization support for AutoDev UI
 *
 * Provides simple i18n without external dependencies
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
}

/**
 * Translation keys and values
 */
object Strings {
    private var currentLanguage = Language.ENGLISH

    // Translation tables
    private val translations =
        mapOf(
            Language.ENGLISH to EnglishStrings,
            Language.CHINESE to ChineseStrings
        )

    /**
     * Set current language
     */
    fun setLanguage(lang: Language) {
        currentLanguage = lang
    }

    /**
     * Get current language
     */
    fun getLanguage(): Language = currentLanguage

    /**
     * Get translated string
     */
    private fun get(key: String): String {
        return translations[currentLanguage]?.get(key) ?: key
    }

    // Common strings
    val save: String get() = get("common.save")
    val cancel: String get() = get("common.cancel")
    val back: String get() = get("common.back")
    val confirm: String get() = get("common.confirm")
    val configure: String get() = get("common.configure")
    val loading: String get() = get("common.loading")

    // Chat UI
    val chatTitle: String get() = get("chat.title")
    val openDirectory: String get() = get("chat.openDirectory")
    val openProject: String get() = get("chat.openProject")
    val newChat: String get() = get("chat.newChat")
    val debugInfo: String get() = get("chat.debugInfo")

    // Model Configuration
    val modelConfigTitle: String get() = get("modelConfig.title")
    val provider: String get() = get("modelConfig.provider")
    val model: String get() = get("modelConfig.model")
    val modelHint: String get() = get("modelConfig.modelHint")
    val apiKey: String get() = get("modelConfig.apiKey")
    val baseUrl: String get() = get("modelConfig.baseUrl")
    val temperature: String get() = get("modelConfig.temperature")
    val maxTokens: String get() = get("modelConfig.maxTokens")
    val advancedParameters: String get() = get("modelConfig.advancedParameters")
    val configureModel: String get() = get("modelConfig.configureModel")
    val noSavedConfigs: String get() = get("modelConfig.noSavedConfigs")
    val enterModel: String get() = get("modelConfig.enterModel")
    val enterApiKey: String get() = get("modelConfig.enterApiKey")
    val showApiKey: String get() = get("modelConfig.showApiKey")
    val hideApiKey: String get() = get("modelConfig.hideApiKey")
    val maxResponseLength: String get() = get("modelConfig.maxResponseLength")
    val temperatureRange: String get() = get("modelConfig.temperatureRange")
    val selected: String get() = get("modelConfig.selected")

    // Messages
    fun failedToLoadConfigs(error: String): String = get("messages.failedToLoadConfigs").replace("{{error}}", error)

    fun failedToSetActiveConfig(error: String): String =
        get("messages.failedToSetActiveConfig").replace("{{error}}", error)

    fun failedToSaveConfig(error: String): String = get("messages.failedToSaveConfig").replace("{{error}}", error)
}

/**
 * English translations
 */
private object EnglishStrings : Map<String, String> by mapOf(
    "common.save" to "Save",
    "common.cancel" to "Cancel",
    "common.back" to "Back",
    "common.confirm" to "Confirm",
    "common.configure" to "Configure",
    "common.loading" to "Loading",
    "chat.title" to "AutoDev",
    "chat.openDirectory" to "Open Directory",
    "chat.openProject" to "Open Project",
    "chat.newChat" to "New Chat",
    "chat.debugInfo" to "Debug Info",
    "modelConfig.title" to "LLM Model Configuration",
    "modelConfig.provider" to "Provider",
    "modelConfig.model" to "Model",
    "modelConfig.apiKey" to "API Key",
    "modelConfig.baseUrl" to "Base URL",
    "modelConfig.temperature" to "Temperature",
    "modelConfig.maxTokens" to "Max Tokens",
    "modelConfig.advancedParameters" to "Advanced Parameters",
    "modelConfig.configureModel" to "Configure Model",
    "modelConfig.noSavedConfigs" to "No saved configurations",
    "modelConfig.enterModel" to "Enter or select model name",
    "modelConfig.modelHint" to "Select from list or type custom model name",
    "modelConfig.enterApiKey" to "Enter your API key",
    "modelConfig.showApiKey" to "Show API key",
    "modelConfig.hideApiKey" to "Hide API key",
    "modelConfig.maxResponseLength" to "Maximum response length",
    "modelConfig.temperatureRange" to "0.0 - 2.0",
    "modelConfig.selected" to "Selected",
    "messages.failedToLoadConfigs" to "Failed to load configs: {{error}}",
    "messages.failedToSetActiveConfig" to "Failed to set active config: {{error}}",
    "messages.failedToSaveConfig" to "Failed to save config: {{error}}"
)

/**
 * Chinese translations (Simplified)
 */
private object ChineseStrings : Map<String, String> by mapOf(
    "common.save" to "保存",
    "common.cancel" to "取消",
    "common.back" to "返回",
    "common.confirm" to "确认",
    "common.configure" to "配置",
    "common.loading" to "加载中",
    "chat.title" to "AutoDev",
    "chat.openDirectory" to "打开目录",
    "chat.openProject" to "打开项目",
    "chat.newChat" to "新建聊天",
    "chat.debugInfo" to "调试信息",
    "modelConfig.title" to "LLM 模型配置",
    "modelConfig.provider" to "提供商",
    "modelConfig.model" to "模型",
    "modelConfig.apiKey" to "API Key",
    "modelConfig.baseUrl" to "Base URL",
    "modelConfig.temperature" to "温度",
    "modelConfig.maxTokens" to "最大令牌数",
    "modelConfig.advancedParameters" to "高级参数",
    "modelConfig.configureModel" to "配置模型...",
    "modelConfig.noSavedConfigs" to "没有保存的配置",
    "modelConfig.enterModel" to "输入或选择模型名称",
    "modelConfig.modelHint" to "从列表中选择或输入自定义模型名称",
    "modelConfig.enterApiKey" to "输入您的 API Key",
    "modelConfig.showApiKey" to "显示 API Key",
    "modelConfig.hideApiKey" to "隐藏 API Key",
    "modelConfig.maxResponseLength" to "最大响应长度",
    "modelConfig.temperatureRange" to "0.0 - 2.0",
    "modelConfig.selected" to "已选择",
    "messages.failedToLoadConfigs" to "加载配置失败：{{error}}",
    "messages.failedToSetActiveConfig" to "设置活动配置失败：{{error}}",
    "messages.failedToSaveConfig" to "保存配置失败：{{error}}"
)
