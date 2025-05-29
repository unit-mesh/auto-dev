package cc.unitmesh.devti.settings.ui

import com.intellij.icons.AllIcons
import java.awt.Component
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer

/**
 * Delete button renderer for table cells
 */
class DeleteButtonRenderer(private val isCustomLLM: (String) -> Boolean) : DefaultTableCellRenderer() {
    private val deleteButton = JButton(AllIcons.Actions.DeleteTag)

    init {
        deleteButton.isOpaque = true
        deleteButton.toolTipText = "Delete"
        deleteButton.isBorderPainted = false
        deleteButton.isContentAreaFilled = false
    }

    override fun getTableCellRendererComponent(
        table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): Component {
        // Only show delete button for custom LLMs (not GitHub Copilot)
        val modelName = table?.getValueAt(row, 0) as? String
        val isCustomModel = isCustomLLM(modelName ?: "")

        if (isCustomModel) {
            deleteButton.background = if (isSelected) table?.selectionBackground else table?.background
            return deleteButton
        } else {
            // For GitHub Copilot models, show empty cell
            val label = JLabel("")
            label.background = if (isSelected) table?.selectionBackground else table?.background
            label.isOpaque = true
            return label
        }
    }
}

/**
 * Delete button editor for table cells
 */
class DeleteButtonEditor(
    private val isCustomLLM: (String) -> Boolean,
    private val onDelete: (Int) -> Unit
) : DefaultCellEditor(JCheckBox()) {
    private val deleteButton = JButton(AllIcons.Actions.DeleteTag)
    private var currentRow = -1

    init {
        deleteButton.toolTipText = "Delete"
        deleteButton.isBorderPainted = false
        deleteButton.isContentAreaFilled = false
        deleteButton.addActionListener {
            // Stop editing and delete the LLM
            stopCellEditing()
            onDelete(currentRow)
        }
    }

    override fun getTableCellEditorComponent(
        table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int
    ): Component {
        currentRow = row
        return deleteButton
    }

    override fun getCellEditorValue(): Any {
        return ""
    }
}