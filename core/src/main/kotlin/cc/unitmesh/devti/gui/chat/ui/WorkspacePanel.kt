package cc.unitmesh.devti.gui.chat.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JPanel

class WorkspacePanel(
    private val project: Project,
    private val input: AutoDevInput
) : JPanel(BorderLayout()) {
    private val workspaceFiles = mutableListOf<VirtualFile>()
    private val filesPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 2))
    
    init {
        border = JBUI.Borders.empty()

        val addButton = JBLabel(AllIcons.General.Add)
        addButton.cursor = java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)
        addButton.toolTipText = "Add files to workspace"
        addButton.border = JBUI.Borders.empty(2, 4)
        addButton.background = JBColor(0xEDF4FE, 0x313741)
        addButton.isOpaque = true
        addButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                addFile()
            }
        })
        
        filesPanel.isOpaque = false
        filesPanel.add(addButton)
        
        add(filesPanel, BorderLayout.CENTER)
        isOpaque = false
    }
    
    private fun addFile() {
        val descriptor = FileChooserDescriptor(true, true, false, false, false, true)
            .withTitle("Select Files for Workspace")
            .withDescription("Choose files to add to your workspace")
        
        FileChooser.chooseFiles(descriptor, project, null) { files ->
            for (file in files) {
                addFileToWorkspace(file)
            }
        }
    }
    
    fun addFileToWorkspace(file: VirtualFile) {
        if (!workspaceFiles.contains(file)) {
            workspaceFiles.add(file)
            updateFilesPanel()

            // Add file reference to the input (will be visible on submit)
            val relativePath = try {
                project.basePath?.let { basePath ->
                    file.path.substringAfter(basePath).removePrefix("/")
                } ?: file.path
            } catch (e: Exception) {
                file.path
            }
            
            input.appendText("\n/file:$relativePath")
        }
    }
    
    private fun updateFilesPanel() {
        filesPanel.removeAll()
        
        val addButton = JBLabel(AllIcons.General.Add)
        addButton.cursor = java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)
        addButton.toolTipText = "Add files to workspace"
        addButton.border = JBUI.Borders.empty(2, 4)
        addButton.background = JBColor(0xEDF4FE, 0x313741)
        addButton.isOpaque = true
        addButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                addFile()
            }
        })
        filesPanel.add(addButton)
        
        for (file in workspaceFiles) {
            val fileLabel = FileItemPanel(project, file) { 
                removeFile(file)
            }
            filesPanel.add(fileLabel)
        }
        
        filesPanel.revalidate()
        filesPanel.repaint()
    }
    
    private fun removeFile(file: VirtualFile) {
        workspaceFiles.remove(file)
        updateFilesPanel()
    }
    
    fun clear() {
        workspaceFiles.clear()
        updateFilesPanel()
    }
}

class FileItemPanel(
    private val project: Project,
    private val file: VirtualFile,
    private val onRemove: () -> Unit
) : JPanel(FlowLayout(FlowLayout.LEFT, 2, 0)) {
    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1, true),
            JBUI.Borders.empty(1, 3)
        )
        background = JBColor(0xEDF4FE, 0x313741)
        isOpaque = true
        
        val icon = file.fileType.icon
        val fileLabel = JBLabel(file.name, icon, JBLabel.LEFT)
        
        val removeLabel = JBLabel(AllIcons.Actions.Close)
        removeLabel.cursor = java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)
        removeLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onRemove()
            }
        })
        
        add(fileLabel)
        add(removeLabel)
        
        this.border = JBUI.Borders.empty(2)
    }
}

