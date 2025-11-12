package cc.unitmesh.devins.db

import cc.unitmesh.devins.ui.platform.BrowserStorage
import cc.unitmesh.devins.ui.platform.console
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ModelConfig Repository for WASM platform
 * Uses browser localStorage to store model configurations
 */
actual class ModelConfigRepository {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Storage model for model configs
     */
    @Serializable
    private data class StoredModelConfig(
        val id: Long,
        val provider: String,
        val modelName: String,
        val apiKey: String?,
        val baseUrl: String?,
        val temperature: Double,
        val maxTokens: Int,
        val isDefault: Boolean,
        val createdAt: Long,
        val updatedAt: Long
    )

    @Serializable
    private data class ConfigStorage(
        val configs: List<StoredModelConfig>,
        val nextId: Long
    )

    actual fun getAllConfigs(): List<ModelConfig> {
        return try {
            val storage = loadStorage()
            storage.configs.map { it.toModelConfig() }
        } catch (e: Exception) {
            console.error("WASM: Error loading configs: ${e.message}")
            emptyList()
        }
    }

    actual fun getDefaultConfig(): ModelConfig? {
        return try {
            val storage = loadStorage()
            storage.configs.firstOrNull { it.isDefault }?.toModelConfig()
        } catch (e: Exception) {
            console.error("WASM: Error getting default config: ${e.message}")
            null
        }
    }

    actual fun getConfigById(id: Long): ModelConfig? {
        return try {
            val storage = loadStorage()
            storage.configs.firstOrNull { it.id == id }?.toModelConfig()
        } catch (e: Exception) {
            console.error("WASM: Error getting config by id: ${e.message}")
            null
        }
    }

    actual fun saveConfig(config: ModelConfig, setAsDefault: Boolean): Long {
        return try {
            val storage = loadStorage()
            val now = getCurrentTimeMillis()
            val newId = storage.nextId

            val newConfig = StoredModelConfig(
                id = newId,
                provider = config.provider.name,
                modelName = config.modelName,
                apiKey = config.apiKey,
                baseUrl = config.baseUrl,
                temperature = config.temperature,
                maxTokens = config.maxTokens,
                isDefault = setAsDefault,
                createdAt = now,
                updatedAt = now
            )

            val updatedConfigs = if (setAsDefault) {
                storage.configs.map { it.copy(isDefault = false) } + newConfig
            } else {
                storage.configs + newConfig
            }

            val newStorage = ConfigStorage(
                configs = updatedConfigs,
                nextId = newId + 1
            )

            saveStorage(newStorage)
            console.log("WASM: Config saved with id: $newId")
            newId
        } catch (e: Exception) {
            console.error("WASM: Error saving config: ${e.message}")
            0L
        }
    }

    actual fun updateConfig(id: Long, config: ModelConfig) {
        try {
            val storage = loadStorage()
            val now = getCurrentTimeMillis()

            val updatedConfigs = storage.configs.map {
                if (it.id == id) {
                    it.copy(
                        provider = config.provider.name,
                        modelName = config.modelName,
                        apiKey = config.apiKey,
                        baseUrl = config.baseUrl,
                        temperature = config.temperature,
                        maxTokens = config.maxTokens,
                        updatedAt = now
                    )
                } else {
                    it
                }
            }

            val newStorage = storage.copy(configs = updatedConfigs)
            saveStorage(newStorage)
            console.log("WASM: Config updated: $id")
        } catch (e: Exception) {
            console.error("WASM: Error updating config: ${e.message}")
        }
    }

    actual fun setDefaultConfig(id: Long) {
        try {
            val storage = loadStorage()
            val now = getCurrentTimeMillis()

            val updatedConfigs = storage.configs.map {
                it.copy(
                    isDefault = it.id == id,
                    updatedAt = if (it.id == id) now else it.updatedAt
                )
            }

            val newStorage = storage.copy(configs = updatedConfigs)
            saveStorage(newStorage)
            console.log("WASM: Default config set to: $id")
        } catch (e: Exception) {
            console.error("WASM: Error setting default config: ${e.message}")
        }
    }

    actual fun deleteConfig(id: Long) {
        try {
            val storage = loadStorage()
            val updatedConfigs = storage.configs.filter { it.id != id }
            val newStorage = storage.copy(configs = updatedConfigs)
            saveStorage(newStorage)
            console.log("WASM: Config deleted: $id")
        } catch (e: Exception) {
            console.error("WASM: Error deleting config: ${e.message}")
        }
    }

    actual fun deleteAllConfigs() {
        try {
            val newStorage = ConfigStorage(configs = emptyList(), nextId = 1)
            saveStorage(newStorage)
            console.log("WASM: All configs deleted")
        } catch (e: Exception) {
            console.error("WASM: Error deleting all configs: ${e.message}")
        }
    }

    private fun loadStorage(): ConfigStorage {
        val content = BrowserStorage.getItem(STORAGE_KEY)
        return if (content != null) {
            try {
                json.decodeFromString<ConfigStorage>(content)
            } catch (e: Exception) {
                console.warn("WASM: Failed to parse config storage, returning empty: ${e.message}")
                ConfigStorage(configs = emptyList(), nextId = 1)
            }
        } else {
            ConfigStorage(configs = emptyList(), nextId = 1)
        }
    }

    private fun saveStorage(storage: ConfigStorage) {
        val content = json.encodeToString(storage)
        BrowserStorage.setItem(STORAGE_KEY, content)
    }

    private fun StoredModelConfig.toModelConfig(): ModelConfig {
        return ModelConfig(
            provider = LLMProviderType.valueOf(this.provider),
            modelName = this.modelName,
            apiKey = this.apiKey ?: "",
            baseUrl = this.baseUrl ?: "",
            temperature = this.temperature,
            maxTokens = this.maxTokens
        )
    }

    /**
     * Get current time in milliseconds
     * Uses kotlinx.datetime for WASM platform
     */
    private fun getCurrentTimeMillis(): Long {
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    }

    actual companion object {
        private const val STORAGE_KEY = "autodev-model-configs"
        private var instance: ModelConfigRepository? = null

        actual fun getInstance(): ModelConfigRepository {
            return instance ?: ModelConfigRepository().also { instance = it }
        }
    }
}


