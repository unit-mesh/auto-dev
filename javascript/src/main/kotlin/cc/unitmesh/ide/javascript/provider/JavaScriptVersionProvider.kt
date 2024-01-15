package cc.unitmesh.ide.javascript.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.ide.javascript.util.LanguageApplicableUtil
import com.intellij.openapi.project.Project

class JavaScriptVersionProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        val language = creationContext.sourceFile?.language ?: return false
        return LanguageApplicableUtil.isWebChatCreationContextSupported(creationContext.sourceFile)
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val preferType = if (LanguageApplicableUtil.isPreferTypeScript(creationContext)) {
            "TypeScript"
        } else {
            "JavaScript"
        }

        return ChatContextItem(
            JavaScriptContextProvider::class,
            "Prefer $preferType language if the used language and toolset are not defined below or in the user messages"
        )
            .let { listOf(it) }
    }
}