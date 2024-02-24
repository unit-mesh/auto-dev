package cc.unitmesh.harmonyos.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.project.Project

class HarmonyOSChatContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return System.getProperty("idea.platform.prefix", "idea") == "DevEcoStudio"
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        var context = "This project is a HarmonyOS project."

        val languageName = creationContext.element?.language?.displayName

        if (languageName == "TypeScript" || languageName == "JavaScript" || languageName == "ArkTS") {
            context += "Which use TypeScript as the main language, and use Flutter like UI framework."
        }

        return listOf(ChatContextItem(HarmonyOSChatContextProvider::class, context))
    }
}
