package cc.unitmesh.devti.provider.builtin

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.project.Project

class LanguageContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.action != ChatActionType.CODE_COMPLETE
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val language = AutoDevSettingsState.getInstance().language

        val text = "You MUST Use $language to reply me!"
        return listOf(ChatContextItem(LanguageContextProvider::class, text))
    }
}