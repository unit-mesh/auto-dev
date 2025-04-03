package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel

class WorkspacePanel(
    private val project: Project,
    private val input: AutoDevInput
) : JPanel(BorderLayout()) {
    private val workspaceFiles = mutableListOf<VirtualFile>()
    private val filesPanel = JPanel()
    
    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()),
            JBUI.Borders.empty(10)
        )
        
        val workspaceLabel = JBLabel(AutoDevBundle.message("chat.panel.workspace.files", "Edit files in your workspace"))
        workspaceLabel.foreground = UIUtil.getContextHelpForeground()
        workspaceLabel.font = Font(workspaceLabel.font.family, Font.PLAIN, 12)
        
        val addButton = JBLabel(AllIcons.General.Add)
        addButton.cursor = java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)
        addButton.toolTipText = "Add files to workspace"
        addButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                addFile()
            }
        })
        
        filesPanel.layout = BoxLayout(filesPanel, BoxLayout.Y_AXIS)
        
        val topPanel = JPanel(BorderLayout())
        topPanel.isOpaque = false
        topPanel.add(workspaceLabel, BorderLayout.WEST)
        topPanel.add(addButton, BorderLayout.EAST)
        
        add(topPanel, BorderLayout.NORTH)
        add(filesPanel, BorderLayout.CENTER)
        isOpaque = false
        filesPanel.isOpaque = false
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
) : JPanel(FlowLayout(FlowLayout.LEFT, 5, 2)) {
    
    init {
        border = JBUI.Borders.empty(2)
        isOpaque = false
        
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
    }
}
