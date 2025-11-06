package cc.unitmesh.devins.db

import cc.unitmesh.llm.ModelConfig

/**
 * ModelConfig 数据访问层 - JS 实现
 * 目前提供空实现，未来可以基于 localStorage 或 IndexedDB 实现
 */
actual class ModelConfigRepository {
    
    actual fun getAllConfigs(): List<ModelConfig> {
        console.warn("ModelConfigRepository not implemented for JS platform")
        return emptyList()
    }
    
    actual fun getDefaultConfig(): ModelConfig? {
        return null
    }
    
    actual fun getConfigById(id: Long): ModelConfig? {
        return null
    }
    
    actual fun saveConfig(config: ModelConfig, setAsDefault: Boolean): Long {
        return 0L
    }
    
    actual fun updateConfig(id: Long, config: ModelConfig) {
        // No-op
    }
    
    actual fun setDefaultConfig(id: Long) {
        // No-op
    }
    
    actual fun deleteConfig(id: Long) {
        // No-op
    }
    
    actual fun deleteAllConfigs() {
        // No-op
    }
    
    actual companion object {
        private var instance: ModelConfigRepository? = null
        
        actual fun getInstance(): ModelConfigRepository {
            return instance ?: ModelConfigRepository().also { instance = it }
        }
    }
}
