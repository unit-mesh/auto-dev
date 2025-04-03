package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.util.canBeAdded
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.NotNull
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class WorkspacePanel(
    private val project: Project,
    private val input: AutoDevInput
) : JPanel(BorderLayout()) {
    private val workspaceFiles = mutableListOf<FilePresentation>()
    private val filesPanel = JPanel(WrapLayout(FlowLayout.LEFT, 2, 2))
    
    init {
        border = JBUI.Borders.empty()

        filesPanel.isOpaque = false
        filesPanel.add(createAddButton())
        
        add(filesPanel, BorderLayout.NORTH)
        isOpaque = false
    }

    private fun createAddButton(): JBLabel {
        val addButton = JBLabel(AllIcons.General.Add)
        addButton.cursor = Cursor(Cursor.HAND_CURSOR)
        addButton.toolTipText = "Add files to workspace"
        addButton.border = JBUI.Borders.empty(2, 4)
        addButton.background = JBColor(0xEDF4FE, 0x313741)
        addButton.isOpaque = true

        addButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                showFileSearchPopup(this@WorkspacePanel)
            }
        })
        return addButton
    }
    
    private fun showFileSearchPopup(component: JComponent) {
        val popup = FileSearchPopup(project) { files ->
            for (file in files) {
                addFileToWorkspace(file)
            }
        }
        popup.show(component)
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
        val filePresentation = FilePresentation.from(project, file)
        if (workspaceFiles.none { it.virtualFile == file }) {
            workspaceFiles.add(filePresentation)
            updateFilesPanel()
        }
    }
    
    private fun updateFilesPanel() {
        filesPanel.removeAll()
        filesPanel.add(createAddButton())
        
        for (filePresentation in workspaceFiles) {
            val fileLabel = FileItemPanel(project, filePresentation) { 
                removeFile(filePresentation)
            }
            filesPanel.add(fileLabel)
        }
        
        filesPanel.revalidate()
        filesPanel.repaint()
    }
    
    private fun removeFile(filePresentation: FilePresentation) {
        workspaceFiles.remove(filePresentation)
        updateFilesPanel()
    }
    
    fun clear() {
        workspaceFiles.clear()
        updateFilesPanel()
    }
    
    fun getAllFiles(): List<FilePresentation> {
        return workspaceFiles.toList()
    }

    fun getAllFilesFormat(): String {
        return workspaceFiles.joinToString(separator = "\n") {
            "\n/file:${it.presentablePath}"
        }
    }
}

class FileSearchPopup(
    private val project: Project,
    private val onFilesSelected: (List<VirtualFile>) -> Unit
) {
    private var popup: JBPopup? = null
    private val fileListModel = DefaultListModel<FileItem>()
    private val fileList = JList(fileListModel)
    private val searchField = JTextField()
    private val contentPanel = JPanel(BorderLayout())
    private val allProjectFiles = mutableListOf<FileItem>()
    private val minPopupSize = Dimension(435, 300)
    
    init {
        loadProjectFiles()
        setupUI()
    }
    
    private fun loadProjectFiles() {
        allProjectFiles.clear()
        EditorHistoryManager.getInstance(project).fileList.forEach { file ->
            if (file.canBeAdded(project)) {
                allProjectFiles.add(FileItem(file, isRecentFile = true))
            }
        }
        
        ProjectFileIndex.getInstance(project).iterateContent { file ->
            if (file.canBeAdded(project) &&
                !ProjectFileIndex.getInstance(project).isUnderIgnored(file) && 
                ProjectFileIndex.getInstance(project).isInContent(file) &&
                !allProjectFiles.any { it.file.path == file.path }) {
                
                allProjectFiles.add(FileItem(file))
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
                    val selectedFiles = fileList.selectedValuesList.map { it.file }
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
                item.file.name.contains(searchText, ignoreCase = true) ||
                item.file.path.contains(searchText, ignoreCase = true)
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
    
    data class FileItem(
        val file: VirtualFile, 
        val isRecentFile: Boolean = false
    ) {
        val icon = file.fileType.icon
        val name = file.name
        val path = file.path
    }
    
    class FileListCellRenderer : ListCellRenderer<FileItem> {
        private val noBorderFocus = BorderFactory.createEmptyBorder(1, 1, 1, 1)
        
        @NotNull
        override fun getListCellRendererComponent(
            list: JList<out FileItem>?,
            value: FileItem?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val panel = JPanel(BorderLayout())
            value?.let {
                // Create a panel with horizontal layout to display file name and path inline
                val infoPanel = JPanel()
                infoPanel.layout = BoxLayout(infoPanel, BoxLayout.X_AXIS)
                infoPanel.isOpaque = false
                
                // File name with icon
                val fileLabel = JBLabel(it.name, it.icon, JBLabel.LEFT)
                fileLabel.border = JBUI.Borders.emptyRight(8)
                
                // Path with smaller, grayed-out text
                val pathLabel = JBLabel(" - ${getRelativePath(it)}", JBLabel.LEFT)
                pathLabel.font = UIUtil.getFont(UIUtil.FontSize.SMALL, pathLabel.font)
                pathLabel.foreground = UIUtil.getContextHelpForeground()
                
                // Special styling for recent files
                if (it.isRecentFile) {
                    fileLabel.foreground = JBColor(0x0087FF, 0x589DF6)
                }
                
                infoPanel.add(fileLabel)
                infoPanel.add(pathLabel)
                infoPanel.add(Box.createHorizontalGlue()) // This makes the layout adapt to width
                
                panel.add(infoPanel, BorderLayout.CENTER)
                
                if (isSelected) {
                    panel.background = list?.selectionBackground
                    panel.foreground = list?.selectionForeground
                } else {
                    panel.background = list?.background
                    panel.foreground = list?.foreground
                }
                
                panel.border = if (cellHasFocus) {
                    UIManager.getBorder("List.focusCellHighlightBorder") ?: noBorderFocus
                } else {
                    noBorderFocus
                }
            }
            
            return panel
        }
        
        private fun getRelativePath(item: FileItem): String {
            // Try to make the path shorter for display purposes
            return try {
                val basePath = item.path.substringBeforeLast(item.name, "")
                if (basePath.isEmpty()) item.path else basePath
            } catch (e: Exception) {
                item.path
            }
        }
    }
}

class FileItemPanel(
    private val project: Project,
    private val filePresentation: FilePresentation,
    private val onRemove: () -> Unit
) : JPanel(FlowLayout(FlowLayout.LEFT, 2, 0)) {
    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1, true),
            JBUI.Borders.empty(1, 3)
        )
        background = JBColor(0xEDF4FE, 0x313741)
        isOpaque = true
        
        val fileLabel = JBLabel(filePresentation.name, filePresentation.icon, JBLabel.LEFT)
        
        val removeLabel = JBLabel(AllIcons.Actions.Close)
        removeLabel.cursor = Cursor(Cursor.HAND_CURSOR)
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

/**
 * FlowLayout subclass that fully supports wrapping of components.
 */
class WrapLayout : FlowLayout {
    constructor() : super()
    constructor(align: Int) : super(align)
    constructor(align: Int, hgap: Int, vgap: Int) : super(align, hgap, vgap)

    /**
     * Returns the preferred dimensions for this layout given the components
     * in the specified target container.
     * @param target the container that needs to be laid out
     * @return the preferred dimensions to lay out the subcomponents of the specified container
     */
    override fun preferredLayoutSize(target: Container): Dimension {
        return layoutSize(target, true)
    }

    /**
     * Returns the minimum dimensions needed to layout the components
     * contained in the specified target container.
     * @param target the container that needs to be laid out
     * @return the minimum dimensions to lay out the subcomponents of the specified container
     */
    override fun minimumLayoutSize(target: Container): Dimension {
        return layoutSize(target, false)
    }

    /**
     * Calculate the dimensions needed to layout the components in the target container
     * @param target the target container
     * @param preferred true for preferred size, false for minimum size
     * @return the dimensions needed for layout
     */
    private fun layoutSize(target: Container, preferred: Boolean): Dimension {
        synchronized(target.treeLock) {
            // Each row must fit within the target container width
            var targetWidth = target.width
            
            if (targetWidth == 0) {
                targetWidth = Int.MAX_VALUE
            }

            val hgap = this.hgap
            val vgap = this.vgap
            val insets = target.insets
            val horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2)
            val maxWidth = targetWidth - horizontalInsetsAndGap

            // Fit components into the calculated width
            var dim = Dimension(0, 0)
            var rowWidth = 0
            var rowHeight = 0

            val count = target.componentCount
            for (i in 0 until count) {
                val m = target.getComponent(i)
                if (m.isVisible) {
                    val d = if (preferred) m.preferredSize else m.minimumSize
                    
                    // If this component doesn't fit in the current row, start a new row
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        dim.width = maxWidth.coerceAtLeast(rowWidth)
                        dim.height += rowHeight + vgap
                        rowWidth = 0
                        rowHeight = 0
                    }

                    // Add component to current row
                    rowWidth += d.width + hgap
                    rowHeight = rowHeight.coerceAtLeast(d.height)
                }
            }

            // Add last row dimensions
            dim.width = maxWidth.coerceAtLeast(rowWidth)
            dim.height += rowHeight + vgap

            // Account for container's insets
            dim.width += horizontalInsetsAndGap
            dim.height += insets.top + insets.bottom + vgap * 2

            return dim
        }
    }
}
