package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.util.lang.JavaVersion

class JavaVersionProvider : ChatContextProvider {
    override fun collect(
        project: Project,
        creationContext: ChatCreationContext
    ): List<ChatContextItem> {
        val psiFile = creationContext.sourceFile
        val containsJavaFile = { psiFile?.containingFile?.virtualFile?.extension?.equals("java", true) ?: false }

        if (!containsJavaFile()) {
            return emptyList()
        }

        val javaVersion = JavaVersion.current()
        val javaVersionStr = "${javaVersion.feature}"

        val chatContextItem = ChatContextItem(
            JavaVersionProvider::class,
            "You are working on a project that uses Java SDK version $javaVersionStr."
        )

        return listOf(chatContextItem)
    }

    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        val sourceFile = creationContext.sourceFile
        if (sourceFile != null && sourceFile.language == JavaLanguage.INSTANCE) {
            return false
        }
        val projectSdk = ProjectRootManager.getInstance(project).projectSdk
        if (projectSdk != null) {
            return projectSdk.sdkType is JavaSdkType
        }

        val module: Module = ModuleUtilCore.findModuleForFile(sourceFile) ?: return false

        val sdk = ModuleRootManager.getInstance(module).sdk

        return sdk != null && sdk.sdkType is JavaSdkType
    }
}