package cc.unitmesh.devti.provider

import cc.unitmesh.devti.llms.tokenizer.Tokenizer
import cc.unitmesh.devti.llms.tokenizer.TokenizerImpl
import cc.unitmesh.devti.prompting.model.FinalCodePrompt
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class PromptStrategy : LazyExtensionInstance<TechStackProvider>() {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementationClass")
    var implementationClass: String? = null

    private val tokenizer: Tokenizer = TokenizerImpl.INSTANCE

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    // The default OpenAI Token will be 4096, we leave 2048 for the code.
    open fun tokenLength(): Int = 8192
    fun count(code: String): Int = tokenizer.count(code)
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
