package cc.unitmesh.devti.provider

import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.prompting.model.PromptConfig
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class ContextPrompter : LazyExtensionInstance<ContextPrompter>() {
    @Attribute("language")
    var language: String? = null

    @Attribute("implementationClass")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    open fun getUIPrompt(): String = ""
    open fun getRequestPrompt(): String = ""
    open fun initContext(actionType: ChatBotActionType, prefixText: String, file: PsiFile?, project: Project) {}

    companion object {
        private val EP_NAME: ExtensionPointName<ContextPrompter> =
            ExtensionPointName.create("cc.unitmesh.contextPrompter")

        fun prompter(lang: String): ContextPrompter? {
            val extensionList = EP_NAME.extensionList
            val contextPrompter = extensionList.filter {
                it.language?.lowercase() == lang.lowercase()
            }

            return if (contextPrompter.isEmpty()) {
                extensionList.first()
            } else {
                contextPrompter.first()
            }
        }
    }
}

