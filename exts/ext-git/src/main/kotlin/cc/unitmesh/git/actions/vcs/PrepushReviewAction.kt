package cc.unitmesh.git.actions.vcs

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditorWithPreview.Layout
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

class PrepushReviewAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val changeListManager = ChangeListManager.getInstance(project)
        val changes = changeListManager.allChanges.toList()

        if (changes.isEmpty()) {
            showNoChangesDialog(project)
            return
        }

        val structureDiagramBuilder = StructureDiagramBuilder(project, changes)
        val mermaidContent = structureDiagramBuilder.build()

        showMermaidDiagramPopup(project, mermaidContent)
    }

    private fun showNoChangesDialog(project: Project) {
        val panel = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(300, 100)
            border = JBUI.Borders.empty(16)
            add(javax.swing.JLabel("No changes detected for review."), BorderLayout.CENTER)
        }

        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, null)
            .setTitle("Prepush Review")
            .setResizable(false)
            .setMovable(true)
            .setRequestFocus(true)
            .setCancelOnClickOutside(true)
            .createPopup()
            .showCenteredInCurrentWindow(project)
    }

    private fun showMermaidDiagramPopup(project: Project, mermaidContent: String) {
        ApplicationManager.getApplication().invokeLater {
            val editor: FileEditor = createEditor(mermaidContent, project)
            editor.selectNotify()

            object : DialogWrapper(project) {
                init {
                    title = "Prepush Review in Diagram"
                    setOKButtonText("Accept")
                    setSize(1200, 800)
                    init()
                }

                override fun doOKAction() {
                    super.doOKAction()
                }

                override fun createCenterPanel(): JComponent {
                    return editor.component
                }

            }.show()
        }
    }

    private fun createEditor(mermaidContent: String, project: Project): FileEditor {
        val virtualFile = LightVirtualFile("mermaid.mmd", mermaidContent)
        virtualFile.putUserData(
            Key.create<Layout>("TextEditorWithPreview.DefaultLayout"),
            Layout.SHOW_EDITOR_AND_PREVIEW
        )

        val editorProvider = FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList.firstOrNull {
            it.javaClass.simpleName == "MermaidEditorWithPreviewProvider" ||
            it.javaClass.simpleName == "MermaidSplitEditorProvider"
        } ?: TextEditorProvider.getInstance()

        val editor: FileEditor = editorProvider.createEditor(project, virtualFile)
        return editor
    }
}
