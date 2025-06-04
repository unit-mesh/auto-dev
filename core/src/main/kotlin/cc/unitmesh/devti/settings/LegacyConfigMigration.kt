package cc.unitmesh.devti.settings

import cc.unitmesh.devti.llm2.model.Auth
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llm2.model.ModelType
import com.intellij.openapi.ui.Messages
import kotlinx.serialization.json.Json

/**
 * Utility class to migrate legacy LLM configurations to the new simplified system
 */
object LegacyConfigMigration {
    
    /**
     * Check if migration is needed and perform it if user agrees
     */
    fun migrateIfNeeded(): Boolean {
        val settings = AutoDevSettingsState.getInstance()
        
        // Check if we have legacy configuration but no new configuration
        @Suppress("DEPRECATION")
        val hasLegacyConfig = settings.customEngineServer.isNotEmpty() || settings.customModel.isNotEmpty()
        val hasNewConfig = settings.customLlms.isNotEmpty() && settings.customLlms != "[]"
        val hasDefaultModel = settings.defaultModelId.isNotEmpty()
        
        if (hasLegacyConfig && (!hasNewConfig || !hasDefaultModel)) {
            val result = Messages.showYesNoDialog(
                "AutoDev has detected legacy LLM configuration. Would you like to migrate it to the new simplified system?\n\n" +
                        "This will:\n" +
                        "• Convert your existing configuration to the new format\n" +
                        "• Set it as your default model\n" +
                        "• Preserve all your settings\n" +
                        "• Make future configuration easier",
                "Migrate LLM Configuration",
                Messages.getQuestionIcon()
            )
            
            if (result == Messages.YES) {
                return performMigration()
            }
        }
        
        return false
    }
    
    /**
     * Perform the actual migration
     */
    private fun performMigration(): Boolean {
        return try {
            val settings = AutoDevSettingsState.getInstance()
            
            // Get existing custom LLMs
            val existingLlms = try {
                LlmConfig.load().toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            // Create LLM config from legacy settings
            @Suppress("DEPRECATION")
            val legacyConfig = createLegacyLlmConfig(settings)
            
            // Add to existing LLMs if not already present
            val existingNames = existingLlms.map { it.name }.toSet()
            if (!existingNames.contains(legacyConfig.name)) {
                existingLlms.add(legacyConfig)
                
                // Save updated LLM list
                val json = Json { prettyPrint = true }
                settings.customLlms = json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(LlmConfig.serializer()),
                    existingLlms
                )
            }
            
            // Set as default model if no default is set
            if (settings.defaultModelId.isEmpty()) {
                settings.defaultModelId = legacyConfig.name
                settings.useDefaultForAllCategories = true
            }
            
            Messages.showInfoMessage(
                "Legacy configuration has been successfully migrated!\n\n" +
                        "Your LLM '${legacyConfig.name}' is now available in the new system and set as the default model.",
                "Migration Complete"
            )
            
            true
        } catch (e: Exception) {
            Messages.showErrorDialog(
                "Failed to migrate legacy configuration: ${e.message}",
                "Migration Failed"
            )
            false
        }
    }
    
    /**
     * Create LlmConfig from legacy settings
     */
    @Suppress("DEPRECATION")
    private fun createLegacyLlmConfig(settings: AutoDevSettingsState): LlmConfig {
        val modelName = settings.customModel.ifEmpty { "legacy-model" }
        val serverUrl = settings.customEngineServer.ifEmpty { "https://api.openai.com/v1/chat/completions" }
        val token = settings.customEngineToken
        val requestFormat = settings.customEngineRequestFormat.ifEmpty {
            """{ "customFields": {"model": "$modelName", "temperature": 0.0, "stream": true} }"""
        }
        val responseFormat = settings.customEngineResponseFormat.ifEmpty {
            "\$.choices[0].delta.content"
        }
        
        return LlmConfig(
            name = "Legacy Configuration",
            description = "Migrated from legacy settings",
            url = serverUrl,
            auth = Auth(
                type = "Bearer",
                token = token
            ),
            requestFormat = requestFormat,
            responseFormat = responseFormat,
            modelType = ModelType.Default
        )
    }
    
    /**
     * Clear legacy settings after successful migration (optional)
     */
    @Suppress("DEPRECATION")
    fun clearLegacySettings() {
        val settings = AutoDevSettingsState.getInstance()
        
        val result = Messages.showYesNoDialog(
            "Would you like to clear the old legacy configuration fields?\n\n" +
                    "This is optional and won't affect functionality, but will clean up your settings.",
            "Clear Legacy Settings",
            Messages.getQuestionIcon()
        )
        
        if (result == Messages.YES) {
            settings.customEngineServer = ""
            settings.customEngineToken = ""
            settings.customModel = ""
            settings.customOpenAiHost = ""
            settings.customEngineRequestFormat = """{ "customFields": {"model": "deepseek-chat", "temperature": 0.0, "stream": true} }"""
            settings.customEngineResponseFormat = "\$.choices[0].delta.content"
            
            Messages.showInfoMessage(
                "Legacy settings have been cleared.",
                "Cleanup Complete"
            )
        }
    }
}
