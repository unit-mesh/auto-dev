package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ui.viewmodel.FileListViewModel
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.Box
import javax.swing.JToolBar
import java.awt.Component
import java.awt.Container

object InputFileToolbar {
    fun createToolbar(project: Project, viewModel: FileListViewModel, input: AutoDevInput): JToolBar {
        val toolbar = JToolBar()
        toolbar.isFloatable = false

        val reminderLabel = JBLabel(AutoDevBundle.message("chat.panel.select.files"))
        reminderLabel.border = JBUI.Borders.empty(5)
        reminderLabel.foreground = UIUtil.getContextHelpForeground()
        toolbar.add(reminderLabel)

        toolbar.add(Box.createHorizontalGlue())

        val findWorkspacePanel: () -> WorkspacePanel? = lambda@{
            var component: Component? = input.parent
            while (component != null) {
                if (component is AutoDevInputSection) {
                    val workspace = component.findComponentOfType(WorkspacePanel::class.java)
                    if (workspace != null) {
                        return@lambda workspace
                    }
                }

                component = component.parent
            }
            null
        }

        val recentFiles = LinkLabel(AutoDevBundle.message("chat.panel.add.openFiles"), null) { _: LinkLabel<Unit>, _: Unit? ->
            val addedFiles = viewModel.addRecentlyOpenedFiles()
            
            val workspace = findWorkspacePanel()
            if (workspace != null) {
                addedFiles.forEach { file ->
                    workspace.addFileToWorkspace(file.virtualFile)
                }
            } else {
                // Fallback to original behavior
                val fileReferences = StringBuilder()
                addedFiles.forEach { vfile ->
                    fileReferences.append("\n/file:${vfile.presentablePath}")
                }
                
                if (fileReferences.isNotEmpty()) {
                    input.appendText(fileReferences.toString())
                }
            }
        }
        
        recentFiles.mediumFontFunction()
        recentFiles.border = JBUI.Borders.emptyRight(10)
        toolbar.add(recentFiles)

        val clearAll = LinkLabel(AutoDevBundle.message("chat.panel.clear.all"), null) { _: LinkLabel<Unit>, _: Unit? ->
            viewModel.clearAllFiles()
            findWorkspacePanel()?.clear()
        }

        clearAll.mediumFontFunction()
        clearAll.border = JBUI.Borders.emptyRight(20)

        toolbar.add(clearAll)

        return toolbar
    }
}

fun <T> Component.findComponentOfType(clazz: Class<T>): T? {
    if (clazz.isInstance(this)) {
        @Suppress("UNCHECKED_CAST")
        return this as T
    }
    
    if (this is Container) {
        for (i in 0 until componentCount) {
            val component = getComponent(i)
            val result = component.findComponentOfType(clazz)
            if (result != null) {
                return result
            }
        }
    }
    
    return null
}
