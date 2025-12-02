package cc.unitmesh.devins.idea.editor

import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.llm.KoogLLMService
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Prompt enhancement service for IntelliJ IDEA.
 * 
 * Enhances user prompts by:
 * 1. Loading domain dictionary from project's prompts directory
 * 2. Loading README file for project context
 * 3. Using LLM to optimize the prompt with domain-specific vocabulary
 * 
 * Based on core/src/main/kotlin/cc/unitmesh/devti/indexer/usage/PromptEnhancer.kt
 */
@Service(Service.Level.PROJECT)
class IdeaPromptEnhancer(private val project: Project) {
    private val logger = Logger.getInstance(IdeaPromptEnhancer::class.java)

    /**
     * Enhance the user's prompt using LLM.
     *
     * @param input The original user prompt
     * @return The enhanced prompt, or the original if enhancement fails
     */
    suspend fun enhance(input: String): String = withContext(Dispatchers.IO) {
        try {
            logger.info("Starting enhancement for input: ${input.take(50)}...")

            val dict = loadDomainDict()
            val readme = loadReadme()
            logger.info("Loaded domain dict (${dict.length} chars), readme (${readme.length} chars)")

            val prompt = buildEnhancePrompt(input, dict, readme)
            logger.info("Built enhancement prompt (${prompt.length} chars)")

            val config = ConfigManager.load()
            val modelConfig = config.getActiveModelConfig()
            if (modelConfig == null) {
                logger.warn("No active model config found, returning original input")
                return@withContext input
            }

            logger.info("Using model: ${modelConfig.modelName}")

            val llmService = KoogLLMService(modelConfig)
            val result = StringBuilder()

            // Use streamPrompt with compileDevIns=false since we're sending a raw prompt
            llmService.streamPrompt(prompt, compileDevIns = false).collect { chunk ->
                result.append(chunk)
            }

            logger.info("LLM response received (${result.length} chars)")
            val enhanced = extractEnhancedPrompt(result.toString())
            if (enhanced != null) {
                logger.info("Extracted enhanced prompt (${enhanced.length} chars)")
            } else {
                logger.warn("Failed to extract enhanced prompt from response")
            }

            enhanced ?: input
        } catch (e: Exception) {
            logger.error("Enhancement failed: ${e.message}", e)
            // Return original input if enhancement fails
            input
        }
    }

    /**
     * Load domain dictionary from project's prompts directory.
     * Looks for domain.csv in the team prompts directory.
     */
    private fun loadDomainDict(): String {
        return try {
            runReadAction {
                val baseDir = project.guessProjectDir() ?: return@runReadAction ""
                val promptsDir = baseDir.findChild(".autodev") ?: baseDir.findChild("prompts")
                val dictFile = promptsDir?.findChild("domain.csv")
                dictFile?.contentsToByteArray()?.toString(Charsets.UTF_8) ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Load README file from project root.
     */
    private fun loadReadme(): String {
        return try {
            runReadAction {
                val baseDir = project.guessProjectDir() ?: return@runReadAction ""
                val readmeFile = baseDir.findChild("README.md")
                    ?: baseDir.findChild("README")
                    ?: baseDir.findChild("readme.md")
                
                val content = readmeFile?.contentsToByteArray()?.toString(Charsets.UTF_8) ?: ""
                // Limit README content to avoid token overflow
                if (content.length > 2000) content.take(2000) + "\n..." else content
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Build the enhancement prompt.
     * Based on core/src/main/resources/genius/en/code/enhance.vm
     */
    private fun buildEnhancePrompt(input: String, dict: String, readme: String): String {
        return buildString {
            appendLine("You are a professional AI prompt optimization expert. Please help me optimize the following prompt and return it in the specified format.")
            appendLine()
            if (dict.isNotBlank()) {
                appendLine("Here is a vocabulary reference provided by the user. Please only consider parts relevant to the user's question.")
                appendLine()
                appendLine("```csv")
                appendLine(dict)
                appendLine("```")
                appendLine()
            }
            if (readme.isNotBlank()) {
                appendLine("Here is the project's README information:")
                appendLine("==========")
                appendLine(readme)
                appendLine("==========")
                appendLine()
            }
            appendLine("Output format requirements:")
            appendLine()
            appendLine("- Return the result in a markdown code block for easy parsing")
            appendLine("- The improved example should be in the same language as the user's prompt")
            appendLine("- The improved example should be consistent with the information described in the user's prompt")
            appendLine("- The output should only contain the improved example, without any other content")
            appendLine("- Only include the improved example, do not add any other content or overly rich content")
            appendLine("- Please do not make extensive associations, just enrich the vocabulary for the user's question")
            appendLine()
            appendLine("Now, the user's question is: $input")
        }
    }

    /**
     * Extract the enhanced prompt from LLM response.
     * Looks for content in markdown code blocks.
     */
    private fun extractEnhancedPrompt(response: String): String? {
        // Try to extract from markdown code block
        val codeBlockRegex = Regex("```(?:\\w+)?\\s*\\n([\\s\\S]*?)\\n```")
        val match = codeBlockRegex.find(response)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        // If no code block, return trimmed response if it's not too long
        val trimmed = response.trim()
        return if (trimmed.length < 500) trimmed else null
    }

    companion object {
        fun getInstance(project: Project): IdeaPromptEnhancer {
            return project.getService(IdeaPromptEnhancer::class.java)
        }
    }
}

