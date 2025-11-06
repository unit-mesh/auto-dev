package cc.unitmesh.llm

import kotlinx.serialization.Serializable

/**
 * 单个命名的 LLM 配置
 * 扩展 ModelConfig，增加 name 字段用于标识和管理多个配置
 */
@Serializable
data class NamedModelConfig(
    val name: String,
    val provider: LLMProviderType = LLMProviderType.DEEPSEEK,
    val modelName: String = "",
    val apiKey: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 8192,
    val baseUrl: String = ""
) {
    /**
     * 转换为基础 ModelConfig（用于实际的 LLM 调用）
     */
    fun toModelConfig(): ModelConfig {
        return ModelConfig(
            provider = provider,
            modelName = modelName,
            apiKey = apiKey,
            temperature = temperature,
            maxTokens = maxTokens,
            baseUrl = baseUrl
        )
    }

    /**
     * 验证配置是否有效
     */
    fun isValid(): Boolean {
        return toModelConfig().isValid()
    }

    companion object {
        /**
         * 从 ModelConfig 创建 NamedModelConfig
         */
        fun from(name: String, config: ModelConfig): NamedModelConfig {
            return NamedModelConfig(
                name = name,
                provider = config.provider,
                modelName = config.modelName,
                apiKey = config.apiKey,
                temperature = config.temperature,
                maxTokens = config.maxTokens,
                baseUrl = config.baseUrl
            )
        }
    }
}

/**
 * 配置文件结构 - 支持多个命名配置和 active 选择
 */
@Serializable
data class ConfigFile(
    val active: String = "",
    val configs: List<NamedModelConfig> = emptyList()
) {
    /**
     * 获取当前激活的配置
     */
    fun getActiveConfig(): NamedModelConfig? {
        if (active.isEmpty() || configs.isEmpty()) {
            return null
        }
        return configs.find { it.name == active } ?: configs.firstOrNull()
    }

    /**
     * 获取激活配置的 ModelConfig（用于 LLM 调用）
     */
    fun getActiveModelConfig(): ModelConfig? {
        return getActiveConfig()?.toModelConfig()
    }

    /**
     * 根据名称查找配置
     */
    fun findConfig(name: String): NamedModelConfig? {
        return configs.find { it.name == name }
    }

    /**
     * 添加或更新配置
     */
    fun withConfig(config: NamedModelConfig, setActive: Boolean = false): ConfigFile {
        val newConfigs = configs.filter { it.name != config.name } + config
        val newActive = if (setActive) config.name else active
        return copy(configs = newConfigs, active = newActive)
    }

    /**
     * 删除配置
     */
    fun withoutConfig(name: String): ConfigFile {
        val newConfigs = configs.filter { it.name != name }
        val newActive = if (active == name && newConfigs.isNotEmpty()) {
            newConfigs.first().name
        } else {
            active
        }
        return copy(configs = newConfigs, active = newActive)
    }

    /**
     * 设置激活配置
     */
    fun withActive(name: String): ConfigFile {
        if (configs.any { it.name == name }) {
            return copy(active = name)
        }
        return this
    }

    /**
     * 验证配置文件是否有效
     */
    fun isValid(): Boolean {
        return getActiveConfig()?.isValid() == true
    }

    companion object {
        /**
         * 创建空配置文件
         */
        fun empty(): ConfigFile {
            return ConfigFile(active = "", configs = emptyList())
        }

        /**
         * 从单个 ModelConfig 创建配置文件（用于向后兼容）
         */
        fun fromSingleConfig(name: String = "default", config: ModelConfig): ConfigFile {
            return ConfigFile(
                active = name,
                configs = listOf(NamedModelConfig.from(name, config))
            )
        }
    }
}
