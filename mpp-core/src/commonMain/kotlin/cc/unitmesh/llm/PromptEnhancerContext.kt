package cc.unitmesh.llm

import cc.unitmesh.indexer.template.TemplateContext
import kotlinx.serialization.Serializable

/**
 * Context for prompt enhancement
 * Contains all necessary data for enhancing user prompts
 */
@Serializable
data class PromptEnhancerContext(
    /**
     * Domain dictionary content (CSV format)
     * Contains domain-specific vocabulary and terminology
     */
    val dict: String = "",
    
    /**
     * User's original input that needs to be enhanced
     */
    val userInput: String,
    
    /**
     * Project README content for context
     * Provides project-specific information
     */
    val readme: String = "",
    
    /**
     * Language preference for the enhancement
     * Defaults to "zh" (Chinese), can be "en" for English
     */
    val language: String = "zh"
) : TemplateContext {

    override fun getProperty(name: String): Any? {
        return when (name) {
            "dict" -> dict
            "userInput" -> userInput
            "readme" -> readme
            "language" -> language
            else -> null
        }
    }

    companion object {
        /**
         * Create a basic context with just user input
         */
        fun basic(userInput: String, language: String = "zh"): PromptEnhancerContext {
            return PromptEnhancerContext(
                userInput = userInput,
                language = language
            )
        }
        
        /**
         * Create a context with domain dictionary
         */
        fun withDict(
            userInput: String, 
            dict: String, 
            language: String = "zh"
        ): PromptEnhancerContext {
            return PromptEnhancerContext(
                dict = dict,
                userInput = userInput,
                language = language
            )
        }
        
        /**
         * Create a full context with all available information
         */
        fun full(
            userInput: String,
            dict: String = "",
            readme: String = "",
            language: String = "zh"
        ): PromptEnhancerContext {
            return PromptEnhancerContext(
                dict = dict,
                userInput = userInput,
                readme = readme,
                language = language
            )
        }
    }
}
