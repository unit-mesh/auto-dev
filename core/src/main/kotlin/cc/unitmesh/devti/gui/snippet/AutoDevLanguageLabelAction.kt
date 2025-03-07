package cc.unitmesh.devti.gui.snippet

import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonLiteral
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.JsonValue
import com.intellij.json.JsonUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent

class AutoDevLanguageLabelAction : DumbAwareAction(), CustomComponentAction {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val languageId = presentation.getClientProperty(LANGUAGE_PRESENTATION_KEY) ?: ""
        val jBLabel: JComponent = JBLabel(languageId)
        jBLabel.setOpaque(false)
        jBLabel.setForeground(UIUtil.getLabelInfoForeground())
        return jBLabel
    }

    override fun updateCustomComponent(component: JComponent, presentation: Presentation) {
        if (component !is JBLabel) return

        val languageId = presentation.getClientProperty(LANGUAGE_PRESENTATION_KEY) ?: ""
        if (languageId.isNotBlank()) {
            component.text = languageId
        }
    }

    override fun actionPerformed(e: AnActionEvent) {

    }

    override fun update(e: AnActionEvent) {
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR) ?: return
        var lightVirtualFile = FileDocumentManager.getInstance().getFile(editor.document) as? LightVirtualFile ?: return

        val project = e.project ?: return
        var displayName =
            lightVirtualFile.language?.displayName ?: CodeFence.displayNameByExt(lightVirtualFile.extension ?: "txt")

        if (lightVirtualFile.language == JsonLanguage.INSTANCE) {
            val content = editor.document.text
            val possibleDevContainer = updateForDevContainer(project, lightVirtualFile, content)
            if (possibleDevContainer != null) {
                displayName = "DevContainer"
            }

            lightVirtualFile = possibleDevContainer ?: lightVirtualFile
        }

        e.presentation.putClientProperty(LANGUAGE_PRESENTATION_KEY, displayName)
    }


    private val DEV_CONTAINER_PROPS =
        setOf("image", "dockerFile", "containerEnv", "remoteUser", "customizations", "features")

    fun isDevContainerProperty(propName: String): Boolean {
        return propName in DEV_CONTAINER_PROPS
    }

    private fun updateForDevContainer(
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

        val image = getEnvObject("image", psiFile) as? JsonStringLiteral
        val dockerfile = getEnvObject("dockerFile", psiFile) as? JsonStringLiteral
        val remoteUser = getEnvObject("remoteUser", psiFile) as? JsonStringLiteral

        val isDevContainer = when {
            image != null && image.value.contains("mcr.microsoft.com/devcontainers") -> true
            dockerfile != null -> true
            remoteUser != null -> true
            rootObject.propertyList.size >= 3 && hasDevContainerProps -> true
            else -> false
        }

        if (!isDevContainer) return null
        val newFile = LightVirtualFile("devcontainer.json", JsonLanguage.INSTANCE, content)

//        try {
//            val providers = JsonSchemaProviderFactory.EP_NAME.extensions.map { it.getProviders(project) }.flatten()
//                .filter { it.isAvailable(newFile) }
//
//            providers.map {
//                it.name
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }

        return newFile
    }

    fun getEnvObject(propName: String, psiFile: PsiFile): JsonValue? {
        val rootObject = (psiFile as? JsonFile)?.topLevelValue as? JsonObject ?: return null
        return rootObject.propertyList.firstOrNull { it.name == propName }?.value
    }

    fun JsonProperty.valueAsString(obj: JsonObject): String? {
        val value = JsonUtil.getPropertyValueOfType(obj, name, JsonLiteral::class.java)
        return when (value) {
            is JsonStringLiteral -> value.value
            else -> value?.text
        }
    }

    companion object {
        val LANGUAGE_PRESENTATION_KEY: Key<String> = Key.create("LanguagePresentationKey")
    }
}
