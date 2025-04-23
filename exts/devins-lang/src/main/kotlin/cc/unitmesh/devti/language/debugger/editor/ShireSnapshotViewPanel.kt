package cc.unitmesh.devti.language.debugger.editor

import cc.unitmesh.devti.language.DevInBundle
import cc.unitmesh.devti.language.debugger.snapshot.UserCustomVariableSnapshot
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.text.SimpleDateFormat
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.table.DefaultTableModel

class ShireSnapshotViewPanel : JPanel(BorderLayout()) {
    private val contentPanel = JBPanel<JBPanel<*>>(BorderLayout())
    private val tableModel = DefaultTableModel(arrayOf("Variable", "Operation", "Value", "Timestamp"), 0)

    init {
        val table = JBTable(tableModel).apply {
            tableHeader.reorderingAllowed = true
            tableHeader.resizingAllowed = true
            setShowGrid(true)
            gridColor = JBColor.PanelBackground
            intercellSpacing = JBUI.size(0, 0)

            val columnModel = columnModel
            columnModel.getColumn(0).preferredWidth = 80
            columnModel.getColumn(1).preferredWidth = 60
            columnModel.getColumn(2).preferredWidth = 300
            columnModel.getColumn(3).preferredWidth = 80

            autoResizeMode = JBTable.AUTO_RESIZE_LAST_COLUMN
        }

        val scrollPane = JBScrollPane(
            table,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        ).apply {
            minimumSize = JBUI.size(0, 160)
            preferredSize = JBUI.size(0, 160)
        }

        setupPanel()
        add(scrollPane, BorderLayout.CENTER)
    }

    private fun setupPanel() {
        contentPanel.background = JBColor(0xF5F5F5, 0x2B2D30)

        val titleLabel = JBLabel(DevInBundle.message("editor.preview.variable.panel")).apply {
            font = JBUI.Fonts.label(14f).asBold()
            fontColor = UIUtil.FontColor.BRIGHTER
            background = JBColor(0xF5F5F5, 0x2B2D30)
            font = JBUI.Fonts.label(14.0f).asBold()
            border = JBUI.Borders.empty(0, 16)
            isOpaque = true
        }

        contentPanel.add(titleLabel, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.NORTH)
    }

    fun updateSnapshots(snapshots: List<UserCustomVariableSnapshot>) {
        if (snapshots.isEmpty()) {
            isVisible = false
            return
        }

        isVisible = true

        tableModel.rowCount = 0
        /// remove all rows
        tableModel.dataVector.removeAllElements()

        snapshots.forEach { snapshot ->
            val operation = snapshot.operations.firstOrNull()
            tableModel.addRow(
                arrayOf(
                    snapshot.variableName,
                    operation?.functionName ?: "",
                    snapshot.value.toString(),
                    formatTimestamp(operation?.timestamp ?: 0)
                )
            )
        }

        revalidate()
        repaint()
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) {
            return "N/A"
        }

        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp)
    }
}
