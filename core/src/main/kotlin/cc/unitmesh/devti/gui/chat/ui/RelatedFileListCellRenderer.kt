package cc.unitmesh.devti.gui.chat.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class RelatedFileListCellRenderer(val project: Project) : ListCellRenderer<ModelWrapper> {
    override fun getListCellRendererComponent(
        list: JList<out ModelWrapper>,
        value: ModelWrapper,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val panel = value.panel ?: JPanel(FlowLayout(FlowLayout.LEFT, 3, 0)).apply {
            accessibleContext.accessibleName = "Element Panel"

            border = JBUI.Borders.empty(2, 5)

            val namePanel = JPanel(layout)
            val iconLabel = JLabel(value.virtualFile.fileType.icon ?: AllIcons.FileTypes.Unknown)
            namePanel.add(iconLabel)

            val nameLabel = JLabel(buildDisplayName(value))
            namePanel.add(nameLabel)
            add(namePanel)

            val closeLabel = JLabel(AllIcons.Actions.Close)
            closeLabel.border = JBUI.Borders.empty()

            add(closeLabel, BorderLayout.EAST)

            value.panel = this
            value.namePanel = namePanel

            this.toolTipText = value.virtualFile.relativePath(project)
        }
        val namePanel = value.namePanel
        if (isSelected) {
            namePanel?.background = list.selectionBackground
            namePanel?.foreground = list.selectionForeground
        } else {
            namePanel?.background = panel.background
            namePanel?.foreground = list.foreground
        }

        return panel
    }

    /**
     * Constructs a display name for the given `ModelWrapper` based on the associated virtual file.
     * If the file name starts with "index." (e.g., index.js, index.ts, index.vue, index.html, index.css),
     * the parent folder name is prepended to the file name to provide more context. Otherwise, the file name is returned as is.
     *
     * @param value The `ModelWrapper` instance containing the virtual file for which the display name is to be constructed.
     * @return A user-friendly, context-aware display name for the file as a `@NlsSafe` string.
     */
    private fun buildDisplayName(value: ModelWrapper): @NlsSafe String {
        val filename = value.virtualFile.name
        // if it's frontend index file,  like index.js, index.ts, index.vue, index.html, index.css etc, then show the parent folder name
        if (filename.startsWith("index.")) {
            val parent = value.virtualFile.parent?.name
            if (parent != null) {
                return "$parent/$filename"
            }
        }

        return filename
    }
}