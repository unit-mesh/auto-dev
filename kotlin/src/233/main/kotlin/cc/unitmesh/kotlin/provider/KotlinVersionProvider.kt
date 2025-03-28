package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.projectStructure.languageVersionSettings

class KotlinVersionProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.sourceFile?.language is KotlinLanguage
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val languageVersionSettings = runReadAction {
            project.languageVersionSettings
        }

        val languageVersion = languageVersionSettings.languageVersion.versionString
        return listOf(ChatContextItem(KotlinVersionProvider::class, "- Kotlin API version: $languageVersion"))
    }
}
