package cc.unitmesh.devti.llms.tokenizer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.EncodingType
import com.knuddels.jtokkit.api.IntArrayList

@Service(Service.Level.APP)
class TokenizerImpl : Tokenizer {
    private val maxTokenLength: Int = 16384
    private var registry: EncodingRegistry? = Encodings.newDefaultEncodingRegistry()
    private var encoding: Encoding = registry?.getEncoding(EncodingType.CL100K_BASE)!!

    override fun getMaxLength(): Int = maxTokenLength

    override fun count(string: String): Int = encoding.countTokens(string)

    override fun tokenize(chunk: String): IntArrayList? {
        return encoding.encode(chunk, maxTokenLength).tokens
    }
}

object TokenizerFactory {
    fun createTokenizer(): Tokenizer =  ApplicationManager.getApplication().getService(TokenizerImpl::class.java)
}