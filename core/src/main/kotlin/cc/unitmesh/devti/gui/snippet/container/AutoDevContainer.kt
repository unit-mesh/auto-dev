package cc.unitmesh.devti.gui.snippet.container

import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.JsonValue
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile

object AutoDevContainer {
    private val DEV_CONTAINER_PROPS =
        setOf("image", "dockerFile", "containerEnv", "remoteUser", "customizations", "features")

    fun isDevContainerProperty(propName: String): Boolean {
        return propName in DEV_CONTAINER_PROPS
    }

    fun updateForDevContainer(
        project: Project,
        lightVirtualFile: LightVirtualFile,
        content: String
    ): LightVirtualFile? {
        val fileName = lightVirtualFile.name.lowercase()
        if ((!content.startsWith("{") && !content.endsWith("}"))) return null

        if (fileName == "devcontainer.json" || fileName.contains("devcontainer")) {
            return lightVirtualFile
        }

        val psiFile = runReadAction { PsiManager.getInstance(project).findFile(lightVirtualFile) } ?: return null
        val rootObject = (psiFile as? JsonFile)?.topLevelValue as? JsonObject ?: return null

        val hasDevContainerProps = rootObject.propertyList.any { isDevContainerProperty(it.name) }

        if (!hasDevContainerProps) return null

        val image = getPropValue("image", psiFile) as? JsonStringLiteral
        val dockerfile = getPropValue("dockerFile", psiFile) as? JsonStringLiteral
        val remoteUser = getPropValue("remoteUser", psiFile) as? JsonStringLiteral

        val isDevContainer = when {
            image != null && image.value.contains("mcr.microsoft.com/devcontainers") -> true
            dockerfile != null -> true
            remoteUser != null -> true
            rootObject.propertyList.size >= 3 && hasDevContainerProps -> true
            else -> false
        }

        if (!isDevContainer) return null
        val newFile = LightVirtualFile("devcontainer.json", JsonLanguage.INSTANCE, content)

        return newFile
    }

    fun getPropValue(propName: String, psiFile: PsiFile): JsonValue? {
        val rootObject = (psiFile as? JsonFile)?.topLevelValue as? JsonObject ?: return null
        return rootObject.propertyList.firstOrNull { it.name == propName }?.value
    }
}