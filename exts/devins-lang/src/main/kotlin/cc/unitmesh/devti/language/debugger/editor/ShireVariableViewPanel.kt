package cc.unitmesh.devti.language.debugger.editor

import cc.unitmesh.devti.language.ast.variable.DebugValueVariable
import cc.unitmesh.devti.language.ast.variable.Variable
import cc.unitmesh.devti.language.debugger.snapshot.VariableSnapshotRecorder
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.util.Vector
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.table.DefaultTableModel

class ShireVariableViewPanel(val project: Project) : JPanel(BorderLayout()) {
    private val contentPanel = JBPanel<JBPanel<*>>(BorderLayout())
    private val tableModel = DefaultTableModel(arrayOf("Name", "Description", "Value"), 0).apply {
        background = JBColor.WHITE
    }

    private val snapshotViewPanel = ShireSnapshotViewPanel()
    private val snapshotRecorder = VariableSnapshotRecorder.getInstance(project)

    init {
        val table = JBTable(tableModel).apply {
            tableHeader.reorderingAllowed = true
            tableHeader.resizingAllowed = true
            setShowGrid(true)
            gridColor = JBColor.PanelBackground
            intercellSpacing = JBUI.size(0, 0)

            val columnModel = columnModel
            columnModel.getColumn(0).preferredWidth = 150
            columnModel.getColumn(1).preferredWidth = 450

            autoResizeMode = JBTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
        }

        val scrollPane = JBScrollPane(
            table,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        ).apply {
            minimumSize = JBUI.size(0, 160)
            preferredSize = JBUI.size(0, 160)
        }

        add(scrollPane, BorderLayout.CENTER)

        add(snapshotViewPanel, BorderLayout.SOUTH)
        setupPanel()
    }

    private fun setupPanel() {
        contentPanel.background = JBColor(0xF5F5F5, 0x2B2D30)

        val titleLabel = JBLabel("Variables").apply {
            font = JBUI.Fonts.label(14f).asBold()
            border = JBUI.Borders.empty(4, 8)
        }

        contentPanel.add(titleLabel, BorderLayout.NORTH)
    }

    fun updateVariables(variables: Map<String, Any>) {
        tableModel.rowCount = 0

        val allVariables: MutableMap<String, Variable> = DebugValueVariable.all().associateBy { it.variableName }.toMutableMap()

        snapshotViewPanel.updateSnapshots(snapshotRecorder.all())

        variables.toSortedMap().forEach { (key, value) ->
            val valueStr = value.toString()
            val description = DebugValueVariable.description(key)

            /// remove existing variables
            if (allVariables.containsKey(key)) {
                allVariables.remove(key)
            }

            tableModel.addRow(Vector<String>().apply {
                add(key)
                add(description)
                add(valueStr)
            })
        }

        allVariables.forEach { (_, value) ->
            tableModel.addRow(Vector<String>().apply {
                add(value.variableName)
                add(value.description)
                add("N/A")
            })
        }

        revalidate()
        repaint()
    }
}