package cc.unitmesh.devti.provider

import cc.unitmesh.devti.gui.chat.ChatBotActionType
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.serviceContainer.LazyExtensionInstance
import com.intellij.util.xmlb.annotations.Attribute

abstract class ContextPrompter : LazyExtensionInstance<ContextPrompter>() {
    protected var action: ChatBotActionType? = null
    protected var selectedText: String = ""
    protected var file: PsiFile? = null
    protected var project: Project? = null
    protected var lang: String = ""
    protected var offset: Int = 0

    open fun initContext(
        actionType: ChatBotActionType,
        selectedText: String,
        file: PsiFile?,
        project: Project,
        offset: Int
    ) {
        this.action = actionType
        this.selectedText = selectedText
        this.file = file
        this.project = project
        this.lang = file?.language?.displayName ?: ""
        this.offset = offset
    }

    @Attribute("language")
    var language: String? = null

    @Attribute("implementationClass")
    var implementationClass: String? = null

    override fun getImplementationClassName(): String? {
        return implementationClass
    }

    open fun displayPrompt(): String = ""
    open fun requestPrompt(): String = ""

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

