package cc.unitmesh.devti.gui.snippet

import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.json.JsonLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
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
        val lightVirtualFile = FileDocumentManager.getInstance().getFile(editor.document) as? LightVirtualFile ?: return

//        val project = e.project ?: return
//        if (lightVirtualFile.language == JsonLanguage.INSTANCE) {
//            checkDevContainer(project, lightVirtualFile)
//        }

        val displayName =
            lightVirtualFile.language.displayName ?: CodeFence.displayNameByExt(lightVirtualFile.extension ?: "txt")
        e.presentation.putClientProperty(LANGUAGE_PRESENTATION_KEY, displayName)
    }

    private fun checkDevContainer(
        project: Project,
        lightVirtualFile: LightVirtualFile
    ) {
        try {
            // follow: https://containers.dev/guide/dockerfile
            // check image, exist, {
            //    "image": "mcr.microsoft.com/devcontainers/base:ubuntu"

            val providers = JsonSchemaProviderFactory.EP_NAME.extensions.map { it.getProviders(project) }.flatten()
                .filter { it.isAvailable(lightVirtualFile) }

            // devcontainer.json
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val LANGUAGE_PRESENTATION_KEY: Key<String> = Key.create("LanguagePresentationKey")
    }
}
