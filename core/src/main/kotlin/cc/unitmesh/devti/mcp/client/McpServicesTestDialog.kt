package cc.unitmesh.devti.mcp.client

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.MatteBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class McpServicesTestDialog(private val project: Project) : DialogWrapper(project) {
    private val loadingPanel = JBLoadingPanel(BorderLayout(), this.disposable)
    private val contentPanel = JPanel()
    private val searchField = JBTextField()
    private val job = SupervisorJob()
    private val isLoading = AtomicBoolean(false)

    // Track server panels and their expansion state
    private val serverPanels = mutableMapOf<String, ServerPanel>()
    private val expandedServers = mutableSetOf<String>()

    init {
        title = AutoDevBundle.message("sketch.mcp.testMcp")

        // Setup content panel with vertical BoxLayout
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)

        // Setup search functionality
        setupSearch()

        init()
        loadServices()
    }

    // Inner class for server panels
    inner class ServerPanel(val server: String, val tools: List<Tool>) {
        val panel = JPanel(BorderLayout())
        val headerPanel = JPanel(BorderLayout())
        val contentPanel = JPanel(BorderLayout())
        val toolsTable: JBTable
        val tableModel: DefaultTableModel

        init {
            // Setup header panel
            headerPanel.background = UIUtil.getPanelBackground().brighter()
            headerPanel.border = CompoundBorder(
                MatteBorder(0, 0, 1, 0, JBColor.border()),
                JBUI.Borders.empty(8, 12)
            )

            val serverLabel = JBLabel(server)
            serverLabel.font = serverLabel.font.deriveFont(Font.BOLD)

            val toolCountLabel = JBLabel("(${tools.size} ${if (tools.size == 1) "tool" else "tools"})")
            toolCountLabel.foreground = JBColor.GRAY
            toolCountLabel.border = JBUI.Borders.emptyLeft(8)

            val expandIcon = JLabel(AllIcons.General.ArrowDown)
            expandIcon.border = JBUI.Borders.emptyRight(8)

            val headerLeft = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
            headerLeft.isOpaque = false
            headerLeft.add(expandIcon)
            headerLeft.add(serverLabel)
            headerLeft.add(toolCountLabel)

            headerPanel.add(headerLeft, BorderLayout.WEST)

            // Make header clickable for expand/collapse
            headerPanel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            headerPanel.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    toggleExpansion()
                }
            })

            // Setup content panel with tools table
            tableModel = DefaultTableModel(arrayOf("Tool Name", "Description"), 0)
            toolsTable = JBTable(tableModel)
            toolsTable.rowHeight = 30
            toolsTable.setShowGrid(false)
            toolsTable.intercellSpacing = Dimension(0, 0)

            // Custom cell renderers
            val toolNameRenderer = object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
                ): Component {
                    val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    border = JBUI.Borders.empty(6, 12, 6, 4)
                    font = font.deriveFont(Font.PLAIN)
                    return component
                }
            }

            val descriptionRenderer = object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
                ): Component {
                    val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    border = JBUI.Borders.empty(6, 4, 6, 12)
                    return component
                }
            }

            toolsTable.getColumnModel().getColumn(0).cellRenderer = toolNameRenderer
            toolsTable.getColumnModel().getColumn(1).cellRenderer = descriptionRenderer
            toolsTable.getColumnModel().getColumn(0).preferredWidth = 200
            toolsTable.getColumnModel().getColumn(1).preferredWidth = 600

            // Populate table
            if (tools.isEmpty()) {
                tableModel.addRow(arrayOf("No tools found", ""))
            } else {
                tools.forEach { tool ->
                    tableModel.addRow(arrayOf(tool.name, tool.description))
                }
            }

            val scrollPane = JBScrollPane(toolsTable)
            scrollPane.border = JBUI.Borders.empty()
            contentPanel.add(scrollPane, BorderLayout.CENTER)

            // Assemble panel
            panel.add(headerPanel, BorderLayout.NORTH)
            panel.add(contentPanel, BorderLayout.CENTER)
            panel.border = MatteBorder(0, 0, 1, 0, JBColor.border())
        }

        fun toggleExpansion() {
            if (expandedServers.contains(server)) {
                expandedServers.remove(server)
                contentPanel.isVisible = false
                (headerPanel.getComponent(0) as JPanel).getComponent(0).let {
                    if (it is JLabel) it.icon = AllIcons.General.ArrowRight
                }
            } else {
                expandedServers.add(server)
                contentPanel.isVisible = true
                (headerPanel.getComponent(0) as JPanel).getComponent(0).let {
                    if (it is JLabel) it.icon = AllIcons.General.ArrowDown
                }
            }

            // Revalidate and repaint the main content panel
            this@McpServicesTestDialog.contentPanel.revalidate()
            this@McpServicesTestDialog.contentPanel.repaint()
        }

        fun setExpanded(expanded: Boolean) {
            if (expanded != expandedServers.contains(server)) {
                toggleExpansion()
            }
        }

        fun applyFilter(searchText: String) {
            if (searchText.isEmpty()) {
                panel.isVisible = true
                return
            }

            val serverMatches = server.lowercase().contains(searchText)
            var anyToolMatches = false

            // Check if any tool matches
            for (i in 0 until tableModel.rowCount) {
                val toolName = tableModel.getValueAt(i, 0).toString()
                val description = tableModel.getValueAt(i, 1)?.toString() ?: ""

                if (toolName.lowercase().contains(searchText) ||
                    description.lowercase().contains(searchText)) {
                    anyToolMatches = true
                    break
                }
            }

            panel.isVisible = serverMatches || anyToolMatches

            // If searching and this panel is visible, make sure it's expanded
            if (panel.isVisible && !expandedServers.contains(server)) {
                setExpanded(true)
            }
        }
    }

    private fun setupSearch() {
        searchField.border = JBUI.Borders.empty(8)
        searchField.emptyText.text = "Search servers or tools..."

        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                applyFilter(searchField.text)
            }
        })
    }

    private fun applyFilter(searchText: String) {
        val text = searchText.lowercase()
        serverPanels.values.forEach { serverPanel ->
            serverPanel.applyFilter(text)
        }

        // Revalidate and repaint
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())

        // Add search field at the top
        val searchPanel = JPanel(BorderLayout())
        searchPanel.border = JBUI.Borders.emptyBottom(8)
        searchPanel.add(searchField, BorderLayout.CENTER)

        // Add scroll pane for content
        val scrollPane = JBScrollPane(contentPanel)
        scrollPane.border = JBUI.Borders.empty()
        scrollPane.preferredSize = Dimension(800, 400)

        // Add components to loading panel
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

                updateServerPanels(serverInfos)

                // Expand all servers by default
                serverInfos.keys.forEach { server ->
                    expandedServers.add(server)
                }

                loadingPanel.stopLoading()
                isLoading.set(false)
            } catch (e: Exception) {
                // Clear existing panels
                contentPanel.removeAll()
                serverPanels.clear()

                // Add error panel
                val errorPanel = JPanel(BorderLayout())
                errorPanel.border = JBUI.Borders.empty(16)

                val errorLabel = JBLabel("Failed to fetch MCP services: ${e.message}")
                errorLabel.icon = AllIcons.General.Error
                errorLabel.foreground = JBColor.RED

                errorPanel.add(errorLabel, BorderLayout.CENTER)
                contentPanel.add(errorPanel)

                loadingPanel.stopLoading()
                isLoading.set(false)

                logger<McpServicesTestDialog>().warn("Failed to fetch MCP services: $e")
            }
        }
    }

    private fun updateServerPanels(serverInfos: Map<String, List<Tool>>) {
        // Run on UI thread
        SwingUtilities.invokeLater {
            // Clear existing panels
            contentPanel.removeAll()
            serverPanels.clear()

            if (serverInfos.isEmpty()) {
                val emptyPanel = JPanel(BorderLayout())
                emptyPanel.border = JBUI.Borders.empty(16)

                val emptyLabel = JBLabel("No servers found")
                emptyLabel.horizontalAlignment = SwingConstants.CENTER

                emptyPanel.add(emptyLabel, BorderLayout.CENTER)
                contentPanel.add(emptyPanel)
                return@invokeLater
            }

            // Create panel for each server
            serverInfos.forEach { (server, tools) ->
                val serverPanel = ServerPanel(server, tools)
                serverPanels[server] = serverPanel
                contentPanel.add(serverPanel.panel)
            }

            // Revalidate and repaint
            contentPanel.revalidate()
            contentPanel.repaint()
        }
    }

    override fun dispose() {
        job.cancel()
        super.dispose()
    }
}