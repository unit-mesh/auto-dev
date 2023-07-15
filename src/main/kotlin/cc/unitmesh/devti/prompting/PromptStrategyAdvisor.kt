package cc.unitmesh.devti.prompting

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.EncodingType

data class FinalPrompt(val prefixCode: String, val suffixCode: String) {}

/**
 * The default OpenAI Token will be 4096, we leave 3072 for the code.
 */
class PromptStrategyAdvisor(val tokenLength: Int = 3072) {
    private var registry: EncodingRegistry? = Encodings.newDefaultEncodingRegistry()
    var encoding: Encoding = registry?.getEncoding(EncodingType.CL100K_BASE)!!

    fun advice(prefixCode: String, suffixCode: String): FinalPrompt {
        val tokenCount: Int = encoding.countTokens(prefixCode)
        if (tokenCount < tokenLength) {
            return FinalPrompt(prefixCode, suffixCode)
        }

        // remove all `import` syntax in java code, should contains with new line
        val importRegexWithNewLine = Regex("import .*;\n")
        val prefixCodeWithoutImport = prefixCode.replace(importRegexWithNewLine, "")
        val tokenCountWithoutImport: Int = encoding.countTokens(prefixCodeWithoutImport)

        if (tokenCountWithoutImport < tokenLength) {
            return FinalPrompt(prefixCodeWithoutImport, suffixCode)
        }

        // keep only the service calling ?
        return FinalPrompt(prefixCodeWithoutImport, suffixCode)
    }
}