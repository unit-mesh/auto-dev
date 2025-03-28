package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.idea.detectLanguageLevel
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager

class JavaVersionProvider : ChatContextProvider {
    override suspend fun collect(
        project: Project,
        creationContext: ChatCreationContext
    ): List<ChatContextItem> {
        val psiFile = creationContext.sourceFile

        psiFile?.containingFile?.virtualFile?.extension?.equals("java", true) ?: return emptyList()

        val languageLevel = detectLanguageLevel(project, psiFile) ?: return emptyList()
        val prompt = "You are working on a project that uses Java SDK version $languageLevel."

        return listOf(ChatContextItem(JavaVersionProvider::class, prompt))
    }

    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        val sourceFile = creationContext.sourceFile ?: return false
        if (sourceFile.language != JavaLanguage.INSTANCE) return false
        if (ProjectRootManager.getInstance(project).projectSdk !is JavaSdkType) return false

        val module: Module = try {
            ModuleUtilCore.findModuleForFile(sourceFile)
        } catch (e: Exception) {
            return false
        } ?: return false

        return ModuleRootManager.getInstance(module).sdk is JavaSdkType
    }
}