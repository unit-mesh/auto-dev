package cc.unitmesh.devins.idea.editor

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * Swing-based top toolbar for the input section.
 * Contains add file button and selected file chips.
 */
class SwingTopToolbar(
    private val project: Project,
    private val onFilesSelected: (List<VirtualFile>) -> Unit
) : JPanel(BorderLayout()) {

    private val fileChipsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
    private val selectedFiles = mutableListOf<SelectedFileItem>()

    init {
        border = JBUI.Borders.empty(4)
        isOpaque = false

        // Left side: Add button
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            isOpaque = false

            val addButton = JButton(AllIcons.General.Add).apply {
                toolTipText = "Add File to Context"
                preferredSize = Dimension(28, 28)
                isBorderPainted = false
                isContentAreaFilled = false
                isFocusPainted = false
                addActionListener { showFileSearchPopup() }
            }
            add(addButton)
        }
        add(leftPanel, BorderLayout.WEST)

        // Center: File chips with horizontal scroll
        fileChipsPanel.isOpaque = false
        val scrollPane = JBScrollPane(fileChipsPanel).apply {
            border = JBUI.Borders.empty()
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
            isOpaque = false
            viewport.isOpaque = false
        }
        add(scrollPane, BorderLayout.CENTER)
    }

    private fun showFileSearchPopup() {
        val popup = SwingFileSearchPopup(project) { files ->
            val newItems = files.map { vf ->
                SelectedFileItem(
                    name = vf.name,
                    path = vf.path,
                    virtualFile = vf,
                    isDirectory = vf.isDirectory
                )
            }
            selectedFiles.addAll(newItems.filter { new -> selectedFiles.none { it.path == new.path } })
            onFilesSelected(files)
            updateFileChips()
        }
        popup.show(this)
    }

    private fun updateFileChips() {
        fileChipsPanel.removeAll()
        selectedFiles.forEach { file ->
            fileChipsPanel.add(createFileChip(file))
        }
        fileChipsPanel.revalidate()
        fileChipsPanel.repaint()
    }

    private fun createFileChip(file: SelectedFileItem): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            isOpaque = true
            background = JBUI.CurrentTheme.ActionButton.hoverBackground()
            border = JBUI.Borders.empty(2, 6)

            val icon = if (file.isDirectory) AllIcons.Nodes.Folder else AllIcons.FileTypes.Any_type
            add(JBLabel(file.name, icon, SwingConstants.LEFT))

            val removeButton = JBLabel(AllIcons.Actions.Close).apply {
                toolTipText = "Remove"
                cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        removeFile(file)
                    }
                })
            }
            add(removeButton)
        }
    }

    fun removeFile(file: SelectedFileItem) {
        selectedFiles.removeIf { it.path == file.path }
        updateFileChips()
    }

    fun getSelectedFiles(): List<SelectedFileItem> = selectedFiles.toList()

    fun clearFiles() {
        selectedFiles.clear()
        updateFileChips()
    }
}

