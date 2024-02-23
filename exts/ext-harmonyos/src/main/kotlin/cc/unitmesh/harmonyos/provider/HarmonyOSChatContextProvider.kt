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
        return listOf(ChatContextItem(HarmonyOSChatContextProvider::class, "This project is a HarmonyOS project."))
    }
}
