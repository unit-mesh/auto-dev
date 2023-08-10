package cc.unitmesh.devti.provider

import cc.unitmesh.devti.llms.tokenizer.Tokenizer
import cc.unitmesh.devti.llms.tokenizer.TokenizerImpl
import cc.unitmesh.devti.prompting.CodePromptText
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class PromptStrategy : LazyExtensionInstance<PromptStrategy>() {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementationClass")
    var implementationClass: String? = null

    private val tokenizer: Tokenizer = TokenizerImpl.INSTANCE

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    open fun tokenLength(): Int = AutoDevSettingsState.maxTokenLength
    fun count(code: String): Int = tokenizer.count(code)
    abstract fun advice(prefixCode: String, suffixCode: String): CodePromptText
    abstract fun advice(psiFile: PsiElement, calleeName: String = ""): CodePromptText
    abstract fun advice(psiFile: PsiElement, usedMethod: List<String>, noExistMethods: List<String>): CodePromptText

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
