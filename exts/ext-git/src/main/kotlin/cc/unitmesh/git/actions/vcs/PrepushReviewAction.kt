package cc.unitmesh.git.actions.vcs

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditorWithPreview.Layout
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import kotlin.jvm.javaClass

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

            val component = editor.component

            val scrollPane = JBScrollPane(component).apply {
                preferredSize = Dimension(800, 600)
                border = JBUI.Borders.empty()
            }

            val mainPanel = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(8)
                add(scrollPane, BorderLayout.CENTER)
            }

            val popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, component)
                .setTitle("Code Structure Changes Review")
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setMinSize(Dimension(600, 400))
                .createPopup()

            popup.addListener(object : com.intellij.openapi.ui.popup.JBPopupListener {
                override fun onClosed(event: com.intellij.openapi.ui.popup.LightweightWindowEvent) {
                    Disposer.dispose(editor)
                }
            })

            popup.showCenteredInCurrentWindow(project)
        }
    }

    private fun createEditor(
        mermaidContent: String,
        project: Project
    ): FileEditor {
        val virtualFile = LightVirtualFile("mermaid.mmd", mermaidContent)
        virtualFile.putUserData(
            Key.create<Layout>("TextEditorWithPreview.DefaultLayout"),
            Layout.SHOW_EDITOR_AND_PREVIEW
        )

        val editorProvider = FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList.firstOrNull {
            it.javaClass.simpleName == "MermaidSplitEditorProvider" ||
            it.javaClass.simpleName == "MermaidEditorWithPreviewProvider"
        } ?: TextEditorProvider.getInstance()

        val editor: FileEditor = editorProvider.createEditor(project, virtualFile)
        return editor
    }
}
