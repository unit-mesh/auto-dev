package cc.unitmesh.devti.gui.chat.ui.file

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.toolbar.McpConfigAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor

class WorkspaceFilePanel(private val project: Project) : JPanel(BorderLayout()) {
    private val workspaceFiles = mutableListOf<FilePresentation>()
    private val filesPanel = JPanel(WrapLayout(FlowLayout.LEFT, 2, 2))

    init {
        border = JBUI.Borders.empty()

        filesPanel.isOpaque = false
        filesPanel.add(createAddButton())
        filesPanel.add(createMcpConfigButton())

        add(filesPanel, BorderLayout.NORTH)
        isOpaque = false
    }

    private fun createMcpConfigButton(): JComponent {
        return createActionButton(McpConfigAction())
    }

    private fun createActionButton(action: AnAction, actionPlace: String = ActionPlaces.UNKNOWN): ActionButton {
        val component = ActionButton(
            action,
            action.templatePresentation.clone(),
            actionPlace,
            ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        )
        return component
    }

    private fun createAddButton(): JBLabel {
        val button = JBLabel(AllIcons.General.Add).apply { cursor = Cursor(Cursor.HAND_CURSOR) }
        button.toolTipText = AutoDevBundle.message("chat.panel.add.files.tooltip")
        button.border = JBUI.Borders.empty(2)
        button.background = JBColor(0xEDF4FE, 0x313741)
        button.isOpaque = true

        button.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                showFileSearchPopup(this@WorkspaceFilePanel)
            }
        })

        return button
    }

    private fun showFileSearchPopup(component: JComponent) {
        val popup = WorkspaceFileSearchPopup(project) { files ->
            for (file in files) {
                addFileToWorkspace(file)
            }
        }
        popup.show(component)
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
        filesPanel.add(createMcpConfigButton())

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

    fun getAllFilesFormat(): String {
        return workspaceFiles.joinToString(separator = "\n") {
            "/file:${it.presentablePath}"
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
        removeLabel.toolTipText = AutoDevBundle.message("chat.panel.remove.file.tooltip")
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
