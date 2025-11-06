package cc.unitmesh.devins.db

import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig

/**
 * ModelConfig 数据访问层 - JVM 实现
 */
actual class ModelConfigRepository(private val database: DevInsDatabase) {
    
    private val queries = database.modelConfigQueries
    
    /**
     * 获取所有配置
     */
    actual fun getAllConfigs(): List<ModelConfig> {
        return queries.selectAll().executeAsList().map { it.toModelConfig() }
    }
    
    /**
     * 获取默认配置
     */
    actual fun getDefaultConfig(): ModelConfig? {
        return queries.selectDefault().executeAsOneOrNull()?.toModelConfig()
    }

    /**
     * 根据 ID 获取配置
     */
    actual fun getConfigById(id: Long): ModelConfig? {
        return queries.selectById(id).executeAsOneOrNull()?.toModelConfig()
    }

    /**
     * 保存配置
     */
    actual fun saveConfig(config: ModelConfig, setAsDefault: Boolean): Long {
        val now = System.currentTimeMillis()
        
        queries.insert(
            provider = config.provider.name,
            modelName = config.modelName,
            apiKey = config.apiKey,
            baseUrl = config.baseUrl,
            temperature = config.temperature,
            maxTokens = config.maxTokens.toLong(),
            createdAt = now,
            updatedAt = now,
            isDefault = if (setAsDefault) 1 else 0
        )
        
        // 如果设置为默认，清除其他配置的默认标记
        if (setAsDefault) {
            val lastInsertedId = queries.selectAll().executeAsList().maxByOrNull { it.id }?.id ?: 0
            setDefaultConfig(lastInsertedId)
        }
        
        return queries.selectAll().executeAsList().maxByOrNull { it.id }?.id ?: 0
    }
    
    /**
     * 更新配置
     */
    actual fun updateConfig(id: Long, config: ModelConfig) {
        val now = System.currentTimeMillis()
        
        queries.update(
            provider = config.provider.name,
            modelName = config.modelName,
            apiKey = config.apiKey,
            baseUrl = config.baseUrl,
            temperature = config.temperature.toDouble(),
            maxTokens = config.maxTokens.toLong(),
            updatedAt = now,
            id = id
        )
    }
    
    /**
     * 设置默认配置
     */
    actual fun setDefaultConfig(id: Long) {
        queries.clearDefault()
        queries.setDefault(updatedAt = System.currentTimeMillis(), id = id)
    }

    /**
     * 删除配置
     */
    actual fun deleteConfig(id: Long) {
        queries.delete(id)
    }

    /**
     * 清空所有配置
     */
    actual fun deleteAllConfigs() {
        queries.deleteAll()
    }
    
    /**
     * 数据库模型转换为领域模型
     */
    private fun cc.unitmesh.devins.db.ModelConfig.toModelConfig(): ModelConfig {
        return ModelConfig(
            provider = LLMProviderType.valueOf(this.provider),
            modelName = this.modelName,
            apiKey = this.apiKey,
            baseUrl = this.baseUrl,
            temperature = this.temperature,
            maxTokens = this.maxTokens.toInt()
        )
    }
    
    actual companion object {
        private var instance: ModelConfigRepository? = null

        /**
         * 获取单例实例
         */
        actual fun getInstance(): ModelConfigRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val driverFactory = DatabaseDriverFactory()
                    val database = createDatabase(driverFactory)
                    ModelConfigRepository(database).also { instance = it }
                }
            }
        }
    }
}
