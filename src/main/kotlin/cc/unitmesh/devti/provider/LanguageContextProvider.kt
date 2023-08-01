package cc.unitmesh.devti.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.project.Project

class LanguageContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return true
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val language = AutoDevSettingsState.getInstance().language

        return listOf(
            ChatContextItem(
                LanguageContextProvider::class,
                "You MUST Use $language to return your answer!"
            )
        )
    }
}