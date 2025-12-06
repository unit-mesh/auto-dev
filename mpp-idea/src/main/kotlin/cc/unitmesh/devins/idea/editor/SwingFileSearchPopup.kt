package cc.unitmesh.devins.idea.editor

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * Swing-based file search popup using JBPopup.
 * Shows recent files and allows searching for files/folders.
 */
class SwingFileSearchPopup(
    private val project: Project,
    private val onFilesSelected: (List<VirtualFile>) -> Unit
) {
    private var popup: JBPopup? = null
    private val listModel = DefaultListModel<FileItem>()
    private val fileList = JBList(listModel)
    private val searchField = SearchTextField()

    init {
        fileList.cellRenderer = FileItemRenderer()
        fileList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    selectCurrentItem()
                }
            }
        })
        fileList.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    selectCurrentItem()
                }
            }
        })

        searchField.addDocumentListener(object : com.intellij.ui.DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) {
                updateList(searchField.text)
            }
        })

        // Load initial recent files
        updateList("")
    }

    private fun selectCurrentItem() {
        val selected = fileList.selectedValue
        if (selected != null) {
            onFilesSelected(listOf(selected.virtualFile))
            popup?.cancel()
        }
    }

    private fun updateList(query: String) {
        listModel.clear()
        val items = if (query.length >= 2) {
            searchFiles(query)
        } else {
            loadRecentFiles()
        }
        items.forEach { listModel.addElement(it) }
    }

    private fun loadRecentFiles(): List<FileItem> {
        val result = mutableListOf<FileItem>()
        try {
            ApplicationManager.getApplication().runReadAction {
                val fileList = EditorHistoryManager.getInstance(project).fileList
                fileList.take(15)
                    .filter { it.isValid && !it.isDirectory }
                    .forEach { file ->
                        result.add(FileItem(file, file.name, getRelativePath(file), true))
                    }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return result
    }

    private fun searchFiles(query: String): List<FileItem> {
        val result = mutableListOf<FileItem>()
        val scope = GlobalSearchScope.projectScope(project)
        val lowerQuery = query.lowercase()

        try {
            ApplicationManager.getApplication().runReadAction {
                // Search by filename
                FilenameIndex.processFilesByName(query, false, scope) { file ->
                    if (result.size < 20) {
                        result.add(FileItem(file, file.name, getRelativePath(file), false))
                    }
                    result.size < 20
                }

                // Fuzzy search
                val existingPaths = result.map { it.virtualFile.path }.toSet()
                ProjectFileIndex.getInstance(project).iterateContent { file ->
                    if (file.name.lowercase().contains(lowerQuery) && 
                        file.path !in existingPaths && 
                        result.size < 30) {
                        result.add(FileItem(file, file.name, getRelativePath(file), false))
                    }
                    result.size < 30
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return result
    }

    private fun getRelativePath(file: VirtualFile): String {
        val basePath = project.basePath ?: return file.path
        return if (file.path.startsWith(basePath)) {
            file.path.removePrefix(basePath).removePrefix("/")
        } else {
            file.path
        }
    }

    fun show(component: Component) {
        val panel = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(400, 350)
            border = JBUI.Borders.empty(8)

            add(searchField, BorderLayout.NORTH)
            add(JBScrollPane(fileList).apply {
                border = JBUI.Borders.emptyTop(8)
            }, BorderLayout.CENTER)
        }

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, searchField)
            .setTitle("Add File to Context")
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .createPopup()

        popup?.showUnderneathOf(component)
    }

    data class FileItem(
        val virtualFile: VirtualFile,
        val name: String,
        val path: String,
        val isRecent: Boolean
    )

    private class FileItemRenderer : ListCellRenderer<FileItem> {
        private val label = JBLabel()

        override fun getListCellRendererComponent(
            list: JList<out FileItem>,
            value: FileItem?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            if (value == null) return label

            val icon = when {
                value.virtualFile.isDirectory -> AllIcons.Nodes.Folder
                value.isRecent -> AllIcons.Vcs.History
                else -> AllIcons.FileTypes.Any_type
            }

            label.icon = icon
            label.text = "<html><b>${value.name}</b> <font color='gray'>${value.path}</font></html>"
            label.border = JBUI.Borders.empty(4, 8)

            if (isSelected) {
                label.background = list.selectionBackground
                label.foreground = list.selectionForeground
                label.isOpaque = true
            } else {
                label.background = list.background
                label.foreground = list.foreground
                label.isOpaque = false
            }

            return label
        }
    }
}

