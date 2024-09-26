package cc.unitmesh.devti.provider

import cc.unitmesh.devti.llms.tokenizer.Tokenizer
import cc.unitmesh.devti.llms.tokenizer.TokenizerFactory
import cc.unitmesh.devti.prompting.CodePromptText
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

/**
 * The `PromptStrategy` class is an abstract class that represents a strategy for providing code prompt advice.
 * It extends the `LazyExtensionInstance` class, which allows for lazy initialization of the strategy instance.
 *
 * @property language The language associated with the strategy.
 * @property implementationClass The fully qualified name of the implementation class.
 * @property tokenizer The tokenizer used for tokenizing code.
 *
 * @constructor Creates a new instance of the `PromptStrategy` class.
 */
abstract class PromptStrategy : LazyExtensionInstance<PromptStrategy>() {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementationClass")
    var implementationClass: String? = null

    private val tokenizer: Tokenizer = TokenizerFactory.createTokenizer()

    override fun getImplementationClassName(): String? = implementationClass

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
