package cc.unitmesh.devti.gui.snippet

import cc.unitmesh.devti.gui.snippet.container.AutoDevContainer
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.json.JsonLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Key
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent

class AutoDevLanguageLabelAction : DumbAwareAction(), CustomComponentAction {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val languageId = presentation.getClientProperty(LANGUAGE_PRESENTATION_KEY) ?: ""
        val label: JComponent = JBLabel(languageId)
        label.setOpaque(false)
        label.setForeground(UIUtil.getLabelInfoForeground())
        label.border = JBUI.Borders.empty(0, 8)
        return label
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
            val possibleDevContainer = AutoDevContainer.updateForDevContainer(project, lightVirtualFile, content)
            if (possibleDevContainer != null) {
                displayName = "DevContainer"
            }

            lightVirtualFile = possibleDevContainer ?: lightVirtualFile
        }

        e.presentation.putClientProperty(LANGUAGE_PRESENTATION_KEY, displayName)
    }

    companion object {
        val LANGUAGE_PRESENTATION_KEY: Key<String> = Key.create("LanguagePresentationKey")
    }
}
