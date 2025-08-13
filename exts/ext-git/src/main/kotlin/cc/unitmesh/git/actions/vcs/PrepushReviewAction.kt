package cc.unitmesh.git.actions.vcs

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditorWithPreview.Layout
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.Action

class PrepushReviewAction : AnAction() {
    companion object {
        private val LAYOUT_KEY: Key<Layout> = Key.create("TextEditorWithPreview.DefaultLayout")
    }

    // Reuse a single virtual file so the preview listens to document changes reliably.
    private var previewFile: LightVirtualFile? = null

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

            object : DialogWrapper(project) {
                private val refreshAction = object : DialogWrapperAction("Refresh") {
                    override fun doAction(e: ActionEvent?) {
                        editor.selectNotify()
                        editor.component.revalidate()
                        editor.component.repaint()
                    }
                }
                init {
                    title = "Prepush Review in Diagram"
                    setOKButtonText("Accept")
                    setSize(1200, 800)

                    init()
                    // Ensure preview providers are attached before notifying selection
                    editor.selectNotify()
                }

                override fun doOKAction() {
                    super.doOKAction()
                }

                override fun createActions(): Array<Action> {
                    return arrayOf(refreshAction, okAction, cancelAction)
                }

                override fun createCenterPanel(): JComponent {
                    return editor.component
                }

                override fun dispose() {
                    Disposer.dispose(editor)
                    super.dispose()
                }
            }.showAndGet()
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
