package cc.unitmesh.devti.provider

import cc.unitmesh.devti.gui.chat.ChatBotActionType
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

interface ContextPrompter {
    fun getUIPrompt(): String
    fun getRequestPrompt(): String
    fun initContext(actionType: ChatBotActionType, prefixText: String, file: PsiFile?, project: Project) {}

    companion object {
        private val EP_NAME: ExtensionPointName<ContextPrompter> =
            ExtensionPointName.create("cc.unitmesh.contextPrompter")

        fun prompter(): ContextPrompter? = EP_NAME.extensionList.firstOrNull()
    }
}

