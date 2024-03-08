package cc.unitmesh.harmonyos.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

class HarmonyOSChatContextProvider : ChatContextProvider {
    private val logger = logger<HarmonyOSChatContextProvider>()

    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return System.getProperty("idea.platform.prefix", "idea") == "DevEcoStudio"
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        var context = "This project is a HarmonyOS project."

        val language = creationContext.sourceFile?.language?.displayName

        if (language == "TypeScript" || language == "JavaScript" || language == "ArkTS") {
            context += "Which use TypeScript (ArkTS) as the main language, and use Flutter like TypeScript UI framework."
        } else if (language == "C" || language == "C/C++" || language == "CCE") {
            context += "Which use C++ as the main language, and NAPI for building native Addons."
        }

        return listOf(ChatContextItem(HarmonyOSChatContextProvider::class, context))
    }
}
