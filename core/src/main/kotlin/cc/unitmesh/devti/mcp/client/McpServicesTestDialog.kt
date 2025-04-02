package cc.unitmesh.devti.mcp.client

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.table.DefaultTableModel

class McpServicesTestDialog(private val project: Project) : DialogWrapper(project) {
    private val loadingPanel = JBLoadingPanel(BorderLayout(), this.disposable)
    private val tableModel = DefaultTableModel(arrayOf("Server", "Tool Name", "Description"), 0)
    private val table = JBTable(tableModel)
    private val job = SupervisorJob()
    private val isLoading = AtomicBoolean(false)

    init {
        title = AutoDevBundle.message("sketch.mcp.testMcp")
        init()

        table.preferredScrollableViewportSize = Dimension(800, 400)
        loadServices()
    }

    override fun createCenterPanel(): JComponent {
        val scrollPane = JBScrollPane(table)
        scrollPane.preferredSize = Dimension(800, 400)

        loadingPanel.add(scrollPane, BorderLayout.CENTER)

        return loadingPanel
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
                val serverInfos = serverManager.collectServerInfos()

                updateTable(serverInfos)
                loadingPanel.stopLoading()
                isLoading.set(false)
            } catch (e: Exception) {
                tableModel.addRow(arrayOf("Error", e.message, ""))
                loadingPanel.stopLoading()
                isLoading.set(false)

                logger<McpServicesTestDialog>().warn("Failed to fetch MCP services: $e")
            }
        }
    }

    private fun updateTable(serverInfos: Map<String, List<Tool>>) {
        tableModel.rowCount = 0

        if (serverInfos.isEmpty()) {
            tableModel.addRow(arrayOf("No servers found", "", ""))
            return
        }

        serverInfos.forEach { (server, tools) ->
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
