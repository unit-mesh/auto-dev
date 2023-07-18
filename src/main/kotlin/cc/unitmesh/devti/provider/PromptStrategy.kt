package cc.unitmesh.devti.provider

import cc.unitmesh.devti.prompting.model.FinalCodePrompt
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.EncodingType

private var registry: EncodingRegistry? = Encodings.newDefaultEncodingRegistry()
private var encoding: Encoding = registry?.getEncoding(EncodingType.CL100K_BASE)!!

abstract class PromptStrategy : LazyExtensionInstance<TechStackProvider>() {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementationClass")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    // The default OpenAI Token will be 4096, we leave 2048 for the code.
    open fun tokenLength(): Int = 2048
    fun countTokens(code: String): Int = encoding.countTokens(code)
    abstract fun advice(prefixCode: String, suffixCode: String): FinalCodePrompt
    abstract fun advice(psiFile: PsiElement, calleeName: String = ""): FinalCodePrompt
    abstract fun advice(psiFile: PsiElement, usedMethod: List<String>, noExistMethods: List<String>): FinalCodePrompt

    companion object {
        private val EP_NAME: ExtensionPointName<PromptStrategy> =
            ExtensionPointName.create("cc.unitmesh.promptStrategy")

        fun strategy(lang: String): PromptStrategy? {
            val extensionList = EP_NAME.extensionList
            val providers = extensionList.filter {
                it.language?.lowercase() == lang.lowercase()
            }

            return if (providers.isEmpty()) {
                extensionList.first()
            } else {
                providers.first()
            }
        }
    }

}
