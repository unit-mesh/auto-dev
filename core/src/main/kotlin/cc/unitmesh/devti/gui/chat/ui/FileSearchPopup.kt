package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.util.canBeAdded
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.NotNull
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class FileSearchPopup(
    private val project: Project,
    private val onFilesSelected: (List<VirtualFile>) -> Unit
) {
    private var popup: JBPopup? = null
    private val fileListModel = DefaultListModel<FilePresentation>()
    private val fileList = JBList(fileListModel)
    private val searchField = JTextField()
    private val contentPanel = JPanel(BorderLayout())
    private val allProjectFiles = mutableListOf<FilePresentation>()
    private val minPopupSize = Dimension(435, 300)

    init {
        loadProjectFiles()
        setupUI()
    }

    private fun loadProjectFiles() {
        allProjectFiles.clear()
        EditorHistoryManager.Companion.getInstance(project).fileList.forEach { file ->
            if (file.canBeAdded(project)) {
                val presentation = FilePresentation.from(project, file)
                presentation.isRecentFile = true
                allProjectFiles.add(presentation)
            }
        }

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

        updateFileList("")
    }

    private fun setupUI() {
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updateSearch()
            override fun removeUpdate(e: DocumentEvent) = updateSearch()
            override fun changedUpdate(e: DocumentEvent) = updateSearch()

            private fun updateSearch() {
                updateFileList(searchField.text)
            }
        })

        fileList.cellRenderer = FileListCellRenderer()
        fileList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedFiles = fileList.selectedValuesList.map { it.virtualFile }
                    if (selectedFiles.isNotEmpty()) {
                        onFilesSelected(selectedFiles)
                        popup?.cancel()
                    }
                }
            }
        })

        contentPanel.add(searchField, BorderLayout.NORTH)
        contentPanel.add(JScrollPane(fileList), BorderLayout.CENTER)
        contentPanel.preferredSize = minPopupSize
    }

    private fun updateFileList(searchText: String) {
        fileListModel.clear()

        val filteredFiles = if (searchText.isBlank()) {
            allProjectFiles
        } else {
            allProjectFiles.filter { item ->
                item.name.contains(searchText, ignoreCase = true) ||
                        item.path.contains(searchText, ignoreCase = true)
            }
        }

        filteredFiles.forEach { fileListModel.addElement(it) }
    }

    fun show(component: JComponent) {
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(contentPanel, searchField)
            .setTitle("Search Files")
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .setFocusable(true)
            .setMinSize(minPopupSize)
            .createPopup()

        val topOffset = (component.border?.getBorderInsets(component)?.top ?: 0)
        val leftOffset = (component.border?.getBorderInsets(component)?.left ?: 0)

        popup?.show(RelativePoint(component, Point(leftOffset, -minPopupSize.height + topOffset)))
    }

    class FileListCellRenderer() : ListCellRenderer<FilePresentation> {
        private val noBorderFocus = BorderFactory.createEmptyBorder(1, 1, 1, 1)

        @NotNull
        override fun getListCellRendererComponent(
            list: JList<out FilePresentation>?,
            value: FilePresentation,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val mainPanel = JPanel(BorderLayout())
            val infoPanel = JPanel(BorderLayout())
            infoPanel.isOpaque = false

            val fileLabel = JBLabel(value.name, value.icon, JBLabel.LEFT)
            fileLabel.border = JBUI.Borders.emptyRight(8)

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
                mainPanel.background = list?.selectionBackground
                mainPanel.foreground = list?.selectionForeground
            } else {
                mainPanel.background = list?.background
                mainPanel.foreground = list?.foreground
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
