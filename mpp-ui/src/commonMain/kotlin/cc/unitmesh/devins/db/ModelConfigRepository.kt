package cc.unitmesh.devins.db

import cc.unitmesh.llm.ModelConfig

/**
 * ModelConfig 数据访问层 - 跨平台接口
 */
expect class ModelConfigRepository {
    
    /**
     * 获取所有配置
     */
    fun getAllConfigs(): List<ModelConfig>
    
    /**
     * 获取默认配置
     */
    fun getDefaultConfig(): ModelConfig?
    
    /**
     * 根据 ID 获取配置
     */
    fun getConfigById(id: Long): ModelConfig?
    
    /**
     * 保存配置
     */
    fun saveConfig(config: ModelConfig, setAsDefault: Boolean = false): Long
    
    /**
     * 更新配置
     */
    fun updateConfig(id: Long, config: ModelConfig)
    
    /**
     * 设置默认配置
     */
    fun setDefaultConfig(id: Long)
    
    /**
     * 删除配置
     */
    fun deleteConfig(id: Long)
    
    /**
     * 清空所有配置
     */
    fun deleteAllConfigs()
    
    companion object {
        /**
         * 获取单例实例
         */
        fun getInstance(): ModelConfigRepository
    }
}
