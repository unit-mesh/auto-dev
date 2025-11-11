package cc.unitmesh.devins.db

import cc.unitmesh.llm.ModelConfig

actual class ModelConfigRepository {
    actual fun getAllConfigs(): List<ModelConfig> {
        println("WASM: ModelConfigRepository.getAllConfigs() - returning empty list")
        return emptyList()
    }

    actual fun getDefaultConfig(): ModelConfig? {
        println("WASM: ModelConfigRepository.getDefaultConfig() - returning null")
        return null
    }

    actual fun getConfigById(id: Long): ModelConfig? {
        println("WASM: ModelConfigRepository.getConfigById() - returning null")
        return null
    }

    actual fun saveConfig(config: ModelConfig, setAsDefault: Boolean): Long {
        println("WASM: ModelConfigRepository.saveConfig() - not implemented")
        return 0L
    }

    actual fun updateConfig(id: Long, config: ModelConfig) {
        println("WASM: ModelConfigRepository.updateConfig() - not implemented")
    }

    actual fun setDefaultConfig(id: Long) {
        println("WASM: ModelConfigRepository.setDefaultConfig() - not implemented")
    }

    actual fun deleteConfig(id: Long) {
        println("WASM: ModelConfigRepository.deleteConfig() - not implemented")
    }

    actual fun deleteAllConfigs() {
        println("WASM: ModelConfigRepository.deleteAllConfigs() - not implemented")
    }

    actual companion object {
        actual fun getInstance(): ModelConfigRepository {
            return ModelConfigRepository()
        }
    }
}

