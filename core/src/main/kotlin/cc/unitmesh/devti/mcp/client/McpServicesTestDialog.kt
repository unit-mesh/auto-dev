package cc.unitmesh.devti.mcp.client

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

class McpServicesTestDialog(private val project: Project) : DialogWrapper(project) {
    private val loadingPanel = JBLoadingPanel(BorderLayout(), this.disposable)
    private val tableModel = GroupableTableModel(arrayOf("Server", "Tool Name", "Description"))
    private val table = JBTable(tableModel)
    private val job = SupervisorJob()
    private val isLoading = AtomicBoolean(false)
    private val searchField = JBTextField()
    private val rowSorter = TableRowSorter<GroupableTableModel>(tableModel)

    // Custom table model that supports grouping
    class GroupableTableModel(columnNames: Array<String>) : DefaultTableModel(columnNames, 0) {
        val groupRows = mutableMapOf<Int, String>() // Maps row index to group name
        val expandedGroups = mutableSetOf<String>() // Set of expanded group names

        fun addGroupRow(groupName: String): Int {
            val rowIndex = rowCount
            addRow(arrayOf(groupName, "", ""))
            groupRows[rowIndex] = groupName
            return rowIndex
        }

        fun isGroupRow(row: Int): Boolean {
            return groupRows.containsKey(row)
        }

        fun getGroupForRow(row: Int): String? {
            if (isGroupRow(row)) {
                return groupRows[row]
            }

            for (i in row downTo 0) {
                if (isGroupRow(i)) {
                    return groupRows[i]
                }
            }

            return null
        }

        fun toggleGroupExpansion(groupName: String) {
            if (expandedGroups.contains(groupName)) {
                expandedGroups.remove(groupName)
            } else {
                expandedGroups.add(groupName)
            }
        }

        fun isGroupExpanded(groupName: String): Boolean {
            return expandedGroups.contains(groupName)
        }
    }

    init {
        title = AutoDevBundle.message("sketch.mcp.testMcp")
        table.preferredScrollableViewportSize = Dimension(800, 400)
        table.rowSorter = rowSorter
        table.rowHeight = 30
        table.setShowGrid(false)
        table.intercellSpacing = Dimension(0, 0)

        setupTableRenderers()
        setupSearch()
        init()
        loadServices()
    }

    private fun setupTableRenderers() {
        val serverRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val panel = BorderLayoutPanel()
                panel.background = if (isSelected) table.selectionBackground else table.background

                val modelRow = table.convertRowIndexToModel(row)
                val isGroupRow = (tableModel as GroupableTableModel).isGroupRow(modelRow)

                if (isGroupRow && column == 0) {
                    val groupName = value.toString()
                    val isExpanded = tableModel.isGroupExpanded(groupName)

                    val icon = if (isExpanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
                    val iconLabel = JLabel(icon)
                    iconLabel.border = JBUI.Borders.empty(0, 4, 0, 8)

                    val textLabel = JLabel(groupName)
                    textLabel.font = textLabel.font.deriveFont(Font.BOLD)

                    panel.add(iconLabel, BorderLayout.WEST)
                    panel.add(textLabel, BorderLayout.CENTER)

                    val toolCount = getToolCountForServer(groupName)
                    if (toolCount > 0) {
                        val countLabel = JLabel("($toolCount)")
                        countLabel.foreground = JBColor.GRAY
                        countLabel.border = JBUI.Borders.emptyLeft(8)
                        panel.add(countLabel, BorderLayout.EAST)
                    }

                    panel.border = JBUI.Borders.empty(4, 8, 4, 0)
                    panel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                    if (isSelected) {
                        panel.background = table.selectionBackground.brighter()
                    } else {
                        panel.background = JBColor.background().brighter()
                    }
                } else {
                    val label = JLabel(value?.toString() ?: "")

                    if (column == 0) {
                        label.border = JBUI.Borders.empty(4, 32, 4, 0)
                    } else {
                        label.border = JBUI.Borders.empty(4, 8, 4, 0)
                    }

                    panel.add(label, BorderLayout.CENTER)
                }

                return panel
            }
        }

        val toolRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                border = JBUI.Borders.empty(4, 8, 4, 0)
                return component
            }
        }

        val descriptionRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                border = JBUI.Borders.empty(4, 8)
                return component
            }
        }

        table.getColumnModel().getColumn(0).cellRenderer = serverRenderer
        table.getColumnModel().getColumn(1).cellRenderer = toolRenderer
        table.getColumnModel().getColumn(2).cellRenderer = descriptionRenderer

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                if (row >= 0) {
                    val modelRow = table.convertRowIndexToModel(row)
                    if (tableModel.isGroupRow(modelRow)) {
                        val groupName = tableModel.getValueAt(modelRow, 0) as String
                        tableModel.toggleGroupExpansion(groupName)
                        updateRowFilter()
                        table.repaint()
                    }
                }
            }
        })
    }

    private fun getToolCountForServer(server: String): Int {
        var count = 0
        for (i in 0 until tableModel.rowCount) {
            if (!tableModel.isGroupRow(i) &&
                tableModel.getValueAt(i, 0) == server &&
                tableModel.getValueAt(i, 1) != "No tools found") {
                count++
            }
        }
        return count
    }

    private fun setupSearch() {
        searchField.border = JBUI.Borders.empty(8)
        searchField.emptyText.text = "Search servers or tools..."

        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                updateRowFilter()
            }
        })
    }

    private fun updateRowFilter() {
        val searchText = searchField.text.lowercase()

        rowSorter.rowFilter = object : RowFilter<GroupableTableModel, Int>() {
            override fun include(entry: RowFilter.Entry<out GroupableTableModel, out Int>): Boolean {
                val modelRow = entry.identifier as Int
                val model = entry.model as GroupableTableModel

                if (model.isGroupRow(modelRow)) {
                    if (searchText.isNotEmpty()) {
                        val groupName = model.getValueAt(modelRow, 0) as String

                        for (i in 0 until model.rowCount) {
                            if (!model.isGroupRow(i) && model.getGroupForRow(i) == groupName) {
                                val server = model.getValueAt(i, 0) as String
                                val toolName = model.getValueAt(i, 1) as String
                                val description = model.getValueAt(i, 2)?.toString() ?: ""

                                if (server.lowercase().contains(searchText) ||
                                    toolName.lowercase().contains(searchText) ||
                                    description.lowercase().contains(searchText)) {
                                    return true
                                }
                            }
                        }
                        return false
                    }
                    return true
                }

                val groupName = model.getGroupForRow(modelRow)
                if (searchText.isNotEmpty()) {
                    val server = model.getValueAt(modelRow, 0) as String
                    val toolName = model.getValueAt(modelRow, 1) as String
                    val description = model.getValueAt(modelRow, 2)?.toString() ?: ""

                    return server.lowercase().contains(searchText) ||
                            toolName.lowercase().contains(searchText) ||
                            description.lowercase().contains(searchText)
                }

                return groupName != null && model.isGroupExpanded(groupName)
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())

        val searchPanel = JPanel(BorderLayout())
        searchPanel.border = EmptyBorder(0, 0, 8, 0)
        searchPanel.add(searchField, BorderLayout.CENTER)

        val scrollPane = JBScrollPane(table)
        scrollPane.preferredSize = Dimension(800, 400)

        loadingPanel.add(searchPanel, BorderLayout.NORTH)
        loadingPanel.add(scrollPane, BorderLayout.CENTER)

        mainPanel.add(loadingPanel, BorderLayout.CENTER)

        return mainPanel
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(850, 500)
    }

    private fun loadServices() {
        if (isLoading.getAndSet(true)) return

        loadingPanel.startLoading()

        AutoDevCoroutineScope.workerScope(project).launch {
            try {
                val serverManager = CustomMcpServerManager.instance(project)
                val serverInfos: Map<String, List<Tool>> = serverManager.collectServerInfos()

                updateTable(serverInfos)

                // Expand all servers by default
                serverInfos.keys.forEach { server ->
                    tableModel.expandedGroups.add(server)
                }
                updateRowFilter()

                loadingPanel.stopLoading()
                isLoading.set(false)
            } catch (e: Exception) {
                tableModel.rowCount = 0
                val errorGroupRow = tableModel.addGroupRow("Error")
                tableModel.addRow(arrayOf("Error", e.message ?: "Unknown error", ""))
                tableModel.expandedGroups.add("Error")
                updateRowFilter()

                loadingPanel.stopLoading()
                isLoading.set(false)

                logger<McpServicesTestDialog>().warn("Failed to fetch MCP services: $e")
            }
        }
    }

    private fun updateTable(serverInfos: Map<String, List<Tool>>) {
        tableModel.rowCount = 0
        tableModel.groupRows.clear()

        if (serverInfos.isEmpty()) {
            val noServersRow = tableModel.addGroupRow("No servers found")
            tableModel.expandedGroups.add("No servers found")
            return
        }

        serverInfos.forEach { (server, tools) ->
            // Add server group row
            val serverRowIndex = tableModel.addGroupRow(server)

            if (tools.isEmpty()) {
                tableModel.addRow(arrayOf(server, "No tools found", ""))
            } else {
                tools.forEach { tool ->
                    tableModel.addRow(arrayOf(server, tool.name, tool.description))
                }
            }
        }
    }

    override fun dispose() {
        job.cancel()
        super.dispose()
    }
}