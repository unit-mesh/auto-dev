package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class McpToolListPanel(private val project: Project) : JPanel() {
    private val mcpServerManager = CustomMcpServerManager.instance(project)
    private val allMcpTools = mutableMapOf<String, List<Tool>>()
    private var currentFilteredMcpTools = mutableMapOf<String, List<Tool>>()
    private var loadingJob: Job? = null
    private val serverLoadingStatus = mutableMapOf<String, Boolean>()
    private val serverPanels = mutableMapOf<String, JPanel>()

    private val textGray = JBColor(0x6B7280, 0x9DA0A8)
    private val headerColor = JBColor(0xF3F4F6, 0x2B2D30)

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = UIUtil.getPanelBackground()
    }

    fun loadTools(content: String, onToolsLoaded: (MutableMap<String, List<Tool>>) -> Unit = {}) {
        loadingJob?.cancel()
        serverLoadingStatus.clear()
        serverPanels.clear()
        allMcpTools.clear()
        currentFilteredMcpTools.clear()

        SwingUtilities.invokeLater {
            removeAll()
            revalidate()
            repaint()
        }

        loadingJob = CoroutineScope(Dispatchers.IO).launch {
            val serverConfigs = mcpServerManager.getEnabledServers(content)

            if (serverConfigs.isNullOrEmpty()) {
                SwingUtilities.invokeLater {
                    showNoServersMessage()
                }
                return@launch
            }

            SwingUtilities.invokeLater {
                serverConfigs.keys.forEach { serverName ->
                    serverLoadingStatus[serverName] = true
                    createServerSection(serverName)
                }
            }

            val jobs = serverConfigs.map { (serverName, serverConfig) ->
                launch {
                    try {
                        val tools = mcpServerManager.collectServerInfo(serverName, serverConfig)
                        
                        synchronized(allMcpTools) {
                            allMcpTools[serverName] = tools
                            currentFilteredMcpTools[serverName] = tools
                        }
                        
                        SwingUtilities.invokeLater {
                            updateServerSection(serverName, tools)
                            serverLoadingStatus[serverName] = false
                        }
                    } catch (e: Exception) {
                        SwingUtilities.invokeLater {
                            showServerError(serverName, e.message ?: "Unknown error")
                            serverLoadingStatus[serverName] = false
                        }
                    }
                }
            }
            
            jobs.forEach { it.join() }
            onToolsLoaded(allMcpTools)
        }
    }

    fun filterTools(searchText: String) {
        currentFilteredMcpTools = if (searchText.isEmpty()) {
            allMcpTools.toMutableMap()
        } else {
            allMcpTools.mapValues { (_, tools) ->
                tools.filter { tool ->
                    tool.name.contains(searchText, ignoreCase = true) ||
                            tool.description?.contains(searchText, ignoreCase = true) == true
                }
            }.toMutableMap()
        }

        SwingUtilities.invokeLater {
            currentFilteredMcpTools.forEach { (serverName, tools) ->
                updateServerSection(serverName, tools)
            }
            revalidate()
            repaint()
        }
    }

    private fun createServerSection(serverName: String) {
        val serverPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty()        }

        val headerPanel = JPanel(BorderLayout()).apply {
            background = headerColor
            border = JBUI.Borders.empty(4)
        }

        val serverLabel = JBLabel(serverName).apply {
            font = JBUI.Fonts.label(14.0f).asBold()
            foreground = UIUtil.getLabelForeground()
        }

        headerPanel.add(serverLabel, BorderLayout.WEST)
        serverPanel.add(headerPanel, BorderLayout.NORTH)

        val toolsPanel = JPanel(GridLayout(0, 3, 4, 4)).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty()
        }

        val loadingLabel = JBLabel("Loading tools from $serverName...").apply {
            font = JBUI.Fonts.label(12.0f)
            foreground = textGray
            horizontalAlignment = SwingConstants.LEFT
            icon = AutoDevIcons.LOADING
            iconTextGap = JBUI.scale(8)
        }

        toolsPanel.add(loadingLabel)
        serverPanel.add(toolsPanel, BorderLayout.CENTER)

        serverPanels[serverName] = toolsPanel

        add(serverPanel)
        revalidate()
        repaint()
    }

    private fun updateServerSection(serverName: String, tools: List<Tool>) {
        val toolsPanel = serverPanels[serverName] ?: return
        toolsPanel.removeAll()

        if (tools.isEmpty()) {
            val noToolsLabel = JBLabel("No tools available for $serverName").apply {
                foreground = textGray
                horizontalAlignment = SwingConstants.LEFT
            }
            toolsPanel.add(noToolsLabel)
        } else {
            tools.forEach { tool ->
                val panel = McpToolListCardPanel(project, serverName, tool)
                toolsPanel.add(panel)
            }
        }

        toolsPanel.revalidate()
        toolsPanel.repaint()
    }

    private fun showServerError(serverName: String, errorMessage: String) {
        val toolsPanel = serverPanels[serverName] ?: return
        toolsPanel.removeAll()

        val errorLabel = JBLabel("Error loading tools: $errorMessage").apply {
            foreground = JBColor.RED
            horizontalAlignment = SwingConstants.LEFT
        }

        toolsPanel.add(errorLabel)
        toolsPanel.revalidate()
        toolsPanel.repaint()
    }

    private fun showNoServersMessage() {
        removeAll()

        val noServersPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(16)
        }

        val noServersLabel = JBLabel("No MCP servers configured. Please check your configuration.").apply {
            foreground = textGray
            horizontalAlignment = SwingConstants.CENTER
        }

        noServersPanel.add(noServersLabel, BorderLayout.CENTER)
        add(noServersPanel)
        revalidate()
        repaint()
    }

    fun dispose() {
        loadingJob?.cancel()
    }

    fun getTools(): Map<String, List<Tool>> = allMcpTools
}
