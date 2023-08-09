package cc.unitmesh.devti.llms.tokenizer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.EncodingType

@Service(Service.Level.APP)
class TokenizerImpl(private val maxTokenLength: Int = 8192) : Tokenizer {
    private var registry: EncodingRegistry? = Encodings.newDefaultEncodingRegistry()
    private var encoding: Encoding = registry?.getEncoding(EncodingType.CL100K_BASE)!!

    override fun getMaxLength(): Int = maxTokenLength

    override fun count(string: String): Int = encoding.countTokens(string)
    override fun tokenize(chunk: String): List<Int> {
        return encoding.encode(chunk, maxTokenLength).tokens
    }

    companion object {
        val INSTANCE = ApplicationManager.getApplication().getService(TokenizerImpl::class.java)
    }
}