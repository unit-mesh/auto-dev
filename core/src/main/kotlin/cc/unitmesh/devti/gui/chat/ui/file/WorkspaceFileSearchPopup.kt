package cc.unitmesh.devti.gui.chat.ui.file

import cc.unitmesh.devti.util.canBeAdded
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.SpeedSearchComparator
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.NotNull
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class WorkspaceFileSearchPopup(
    private val project: Project,
    private val onFilesSelected: (List<VirtualFile>) -> Unit
) {
    private var popup: JBPopup? = null
    private val fileListModel = DefaultListModel<FilePresentation>()
    private val fileList = JBList(fileListModel).apply {
        cellRenderer = FileListCellRenderer()
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    }
    private val searchField = SearchTextField()
    private val contentPanel = JPanel(BorderLayout())
    private val allProjectFiles = mutableListOf<FilePresentation>()
    private val minPopupSize = Dimension(500, 400)

    init {
        loadProjectFiles()
        setupUI()
    }

    private fun loadProjectFiles() {
        allProjectFiles.clear()
        
        // Add recent files with higher priority
        val fileList = EditorHistoryManager.getInstance(project).fileList
        fileList.take(20).forEach { file ->
            if (file.canBeAdded(project)) {
                val presentation = FilePresentation.from(project, file)
                presentation.isRecentFile = true
                allProjectFiles.add(presentation)
            }
        }

        // Add all project files
        ProjectFileIndex.getInstance(project).iterateContent { file ->
            if (file.canBeAdded(project) &&
                !ProjectFileIndex.getInstance(project).isUnderIgnored(file) &&
                ProjectFileIndex.getInstance(project).isInContent(file) &&
                !allProjectFiles.any { it.path == file.path }
            ) {
                allProjectFiles.add(FilePresentation.from(project, file))
            }
            true
        }

        filterFiles("")
    }

    private fun setupUI() {
        // Configure search field
        searchField.textEditor.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                filterFiles(searchField.text.trim())
                if (e.keyCode == KeyEvent.VK_DOWN && fileListModel.size > 0) {
                    fileList.requestFocus()
                    fileList.selectedIndex = 0
                }
            }
        })
        
        // Configure file list
        fileList.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_ENTER -> {
                        selectFiles()
                        e.consume()
                    }
                    KeyEvent.VK_ESCAPE -> {
                        popup?.cancel()
                        e.consume()
                    }
                    KeyEvent.VK_UP -> {
                        if (fileList.selectedIndex == 0) {
                            searchField.requestFocus()
                            e.consume()
                        }
                    }
                }
            }
        })

        fileList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    selectFiles()
                }
            }
        })

        // Setup layout with proper borders and spacing
        contentPanel.border = JBUI.Borders.empty()
        contentPanel.add(searchField, BorderLayout.NORTH)
        contentPanel.add(JBScrollPane(fileList), BorderLayout.CENTER)
        contentPanel.preferredSize = minPopupSize
    }

    private fun filterFiles(query: String) {
        fileListModel.clear()
        val filteredFiles = if (query.isBlank()) {
            allProjectFiles.take(50) // Show first 50 files when no query
        } else {
            allProjectFiles.filter { file ->
                file.name.contains(query, ignoreCase = true) ||
                file.path.contains(query, ignoreCase = true)
            }.take(50)
        }
        
        // Sort files: recent files first, then by name
        val sortedFiles = filteredFiles.sortedWith(compareBy<FilePresentation> { !it.isRecentFile }.thenBy { it.name })
        sortedFiles.forEach { fileListModel.addElement(it) }
        
        // Auto-select first item if available
        if (fileListModel.size > 0) {
            fileList.selectedIndex = 0
        }
    }
    
    private fun selectFiles() {
        val selectedFiles = fileList.selectedValuesList.map { it.virtualFile }
        if (selectedFiles.isNotEmpty()) {
            onFilesSelected(selectedFiles)
            popup?.cancel()
        }
    }

    fun show(component: JComponent) {
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(contentPanel, searchField.textEditor)
            .setTitle("Add Files to Workspace")
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .setFocusable(true)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setMinSize(minPopupSize)
            .createPopup()
            
        popup?.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                // Clean up resources when popup is closed
                allProjectFiles.clear()
                fileListModel.clear()
            }
        })

        // Show popup in best position
//        popup?.showInBestPositionFor(component)
        popup?.showInCenterOf(component)
        
        // Request focus for search field after popup is shown
        SwingUtilities.invokeLater {
            IdeFocusManager.findInstance().requestFocus(searchField.textEditor, false)
        }
    }

    private inner class FileListCellRenderer : ListCellRenderer<FilePresentation> {
        private val noBorderFocus = BorderFactory.createEmptyBorder(1, 1, 1, 1)
        private val speedSearchComparator = SpeedSearchComparator(false)

        @NotNull
        override fun getListCellRendererComponent(
            list: JList<out FilePresentation>,
            value: FilePresentation,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val mainPanel = JPanel(BorderLayout())
            val infoPanel = JPanel(BorderLayout())
            infoPanel.isOpaque = false

            val fileLabel = JBLabel(value.name, value.icon, JBLabel.LEFT)
            fileLabel.border = JBUI.Borders.emptyRight(4)

            val relativePath = value.presentablePath
            val pathLabel = JBLabel(" - $relativePath", JBLabel.LEFT)
            pathLabel.font = UIUtil.getFont(UIUtil.FontSize.SMALL, pathLabel.font)
            pathLabel.foreground = UIUtil.getContextHelpForeground()
            pathLabel.toolTipText = relativePath

            if (value.isRecentFile) {
                fileLabel.foreground = JBColor(0x0087FF, 0x589DF6)
            }

            infoPanel.add(fileLabel, BorderLayout.WEST)
            infoPanel.add(pathLabel, BorderLayout.CENTER)

            mainPanel.add(infoPanel, BorderLayout.CENTER)

            if (isSelected) {
                mainPanel.background = list.selectionBackground
                mainPanel.foreground = list.selectionForeground
            } else {
                mainPanel.background = list.background
                mainPanel.foreground = list.foreground
            }

            mainPanel.border = if (cellHasFocus) {
                UIManager.getBorder("List.focusCellHighlightBorder") ?: noBorderFocus
            } else {
                noBorderFocus
            }

            return mainPanel
        }
    }
}