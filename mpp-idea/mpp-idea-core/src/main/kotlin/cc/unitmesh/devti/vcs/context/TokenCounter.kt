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

    fun countTokens(text: String): Int {
        return try {
            encoding.countTokens(text)
        } catch (e: Exception) {
            estimateTokens(text)
        }
    }

    fun countTokens(texts: List<String>): Int {
        return texts.sumOf { countTokens(it) }
    }

    private fun estimateTokens(text: String): Int {
        return (text.length / 4.0).toInt()
    }

    companion object {
        val DEFAULT = TokenCounter()
    }
}

