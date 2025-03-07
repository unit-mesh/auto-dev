package cc.unitmesh.devti.gui.snippet

import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.ide.scratch.ScratchRootType
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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
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
        if (lightVirtualFile.language == JsonLanguage.INSTANCE) {
            lightVirtualFile = updateForDevContainer(project, lightVirtualFile) ?: lightVirtualFile
        }

        val displayName =
            lightVirtualFile.language?.displayName ?: CodeFence.displayNameByExt(lightVirtualFile.extension ?: "txt")
        e.presentation.putClientProperty(LANGUAGE_PRESENTATION_KEY, displayName)
    }

    private fun updateForDevContainer(
        project: Project,
        lightVirtualFile: LightVirtualFile
    ): LightVirtualFile? {
        val psiFile = runReadAction { PsiManager.getInstance(project).findFile(lightVirtualFile) } ?: return null
        val image = getEnvObject("image", psiFile) ?: return null

        val literal = image as? JsonStringLiteral ?: return null
        val imageValue = literal.value

        if (!imageValue.contains("mcr.microsoft.com/devcontainers")) return null

        /// create new file with name devcontainer.json
        val content = lightVirtualFile.inputStream.readBytes().toString(Charsets.UTF_8)
        val newFile = LightVirtualFile(
            "devcontainer.json",
            JsonLanguage.INSTANCE,
            content
        )

        try {
            // follow: https://containers.dev/guide/dockerfile
            // check image, exist, {
            //    "image": "mcr.microsoft.com/devcontainers/base:ubuntu"

            val providers = JsonSchemaProviderFactory.EP_NAME.extensions.map { it.getProviders(project) }.flatten()
                .filter { it.isAvailable(lightVirtualFile) }

            // devcontainer.json
            providers.map {
                it.name
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

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

    fun JsonObject.findString(name: String): String? {
        val property = findProperty(name) ?: return null
        return property.valueAsString(this)
    }

    companion object {
        val LANGUAGE_PRESENTATION_KEY: Key<String> = Key.create("LanguagePresentationKey")
    }
}
