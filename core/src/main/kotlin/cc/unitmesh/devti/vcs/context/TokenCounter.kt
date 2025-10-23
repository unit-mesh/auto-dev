package cc.unitmesh.devti.vcs.context

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.EncodingType

/**
 * Token counter using jtokkit library.
 * Provides accurate token counting for various LLM models.
 */
class TokenCounter(
    private val encodingType: EncodingType = EncodingType.CL100K_BASE
) {
    private val registry: EncodingRegistry = Encodings.newDefaultEncodingRegistry()
    private val encoding: Encoding = registry.getEncoding(encodingType)

    /**
     * Count tokens in a text string
     */
    fun countTokens(text: String): Int {
        return try {
            encoding.countTokens(text)
        } catch (e: Exception) {
            // Fallback to rough estimation if encoding fails
            estimateTokens(text)
        }
    }

    /**
     * Count tokens in multiple text strings
     */
    fun countTokens(texts: List<String>): Int {
        return texts.sumOf { countTokens(it) }
    }

    /**
     * Estimate tokens using a simple heuristic (fallback method)
     * Roughly 1 token per 4 characters for English text
     */
    private fun estimateTokens(text: String): Int {
        return (text.length / 4.0).toInt()
    }

    companion object {
        /**
         * Default instance using CL100K_BASE encoding (GPT-4, GPT-3.5-turbo)
         */
        val DEFAULT = TokenCounter()

        /**
         * Create a token counter for GPT-4 models
         */
        fun forGpt4(): TokenCounter = TokenCounter(EncodingType.CL100K_BASE)

        /**
         * Create a token counter for GPT-3.5 models
         */
        fun forGpt35(): TokenCounter = TokenCounter(EncodingType.CL100K_BASE)

        /**
         * Create a token counter for older GPT-3 models
         */
        fun forGpt3(): TokenCounter = TokenCounter(EncodingType.P50K_BASE)
    }
}

