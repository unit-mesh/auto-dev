package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.KotlinLanguage

class KotlinVersionProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.sourceFile?.language is KotlinLanguage
    }

    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        val languageVersionSettings = runReadAction {
            project.languageVersionSettings
        } ?: return emptyList()

        val languageVersion = languageVersionSettings.languageVersion.versionString
        return listOf(ChatContextItem(KotlinVersionProvider::class, "- Kotlin API version: $languageVersion"))
    }
}

private val Project.languageVersionSettings: LanguageVersionSettings?
    get() {
        if (serviceOrNull<ProjectFileIndex>() == null) {
            return LanguageVersionSettingsImpl.DEFAULT
        }

        return null
    }

