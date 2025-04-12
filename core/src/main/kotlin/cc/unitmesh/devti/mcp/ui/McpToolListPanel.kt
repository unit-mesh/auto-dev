package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
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
import javax.swing.*
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class McpToolListPanel(private val project: Project) : JPanel() {
    private val mcpServerManager = CustomMcpServerManager.instance(project)
    private val allTools = mutableMapOf<String, List<Tool>>()
    private var loadingJob: Job? = null
    private val serverLoadingStatus = mutableMapOf<String, Boolean>()
    private val serverPanels = mutableMapOf<String, JPanel>()
    private val searchField = SearchTextField()
    private var currentFilterText = ""
    
    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41)
    private val textGray = JBColor(0x6B7280, 0x9DA0A8)
    private val headerColor = JBColor(0xF3F4F6, 0x2B2D30)
    
    init {
        layout = BorderLayout()
        background = UIUtil.getPanelBackground()
        
        val searchPanel = createSearchPanel()
        add(searchPanel, BorderLayout.NORTH)
        
        val contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = UIUtil.getPanelBackground()
        }
        add(JScrollPane(contentPanel), BorderLayout.CENTER)
    }
    
    private fun createSearchPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
                JBUI.Borders.empty()
            )
            
            searchField.apply {
                textEditor.document.addDocumentListener(object : DocumentListener {
                    override fun insertUpdate(e: DocumentEvent) = updateFilter()
                    override fun removeUpdate(e: DocumentEvent) = updateFilter()
                    override fun changedUpdate(e: DocumentEvent) = updateFilter()
                })
            }
            
            val searchInnerPanel = JPanel(BorderLayout(JBUI.scale(4), 0)).apply {
                background = UIUtil.getPanelBackground()
                add(searchField, BorderLayout.CENTER)
                border = JBUI.Borders.empty(4)
            }
            
            add(searchInnerPanel, BorderLayout.CENTER)
        }
    }
    
    private fun updateFilter() {
        val filterText = searchField.text.trim().lowercase()
        
        if (filterText == currentFilterText) return
        currentFilterText = filterText
        
        SwingUtilities.invokeLater {
            allTools.forEach { (serverName, tools) ->
                updateServerSection(serverName, tools, filterText)
            }
        }
    }
    
    fun resetSearch() {
        searchField.text = ""
        currentFilterText = ""
        allTools.forEach { (serverName, tools) ->
            updateServerSection(serverName, tools)
        }
    }
    
    fun loadTools(content: String, onToolsLoaded: (MutableMap<String, List<Tool>>) -> Unit = {}) {
        loadingJob?.cancel()
        serverLoadingStatus.clear()
        serverPanels.clear()
        allTools.clear()
        currentFilterText = ""
        searchField.text = ""
        
        SwingUtilities.invokeLater {
            val contentPanel = getContentPanel()
            contentPanel.removeAll()
            contentPanel.revalidate()
            contentPanel.repaint()
        }
        
        loadingJob = CoroutineScope(Dispatchers.IO).launch {
            val serverConfigs = mcpServerManager.getServerConfigs(content)
            
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
            
            serverConfigs.forEach { (serverName, serverConfig) ->
                try {
                    val tools = mcpServerManager.collectServerInfo(serverName, serverConfig)
                    allTools[serverName] = tools
                    onToolsLoaded(allTools)
                    
                    SwingUtilities.invokeLater {
                        updateServerSection(serverName, tools, currentFilterText)
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
    }
    
    private fun getContentPanel(): JPanel {
        return (components.find { it is JScrollPane } as JScrollPane).viewport.view as JPanel
    }
    
    private fun createServerSection(serverName: String) {
        val serverPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
                JBUI.Borders.empty()
            )
        }
        
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
        
        getContentPanel().add(serverPanel)
        getContentPanel().revalidate()
        getContentPanel().repaint()
    }
    
    private fun updateServerSection(serverName: String, tools: List<Tool>, filterText: String = "") {
        val toolsPanel = serverPanels[serverName] ?: return
        toolsPanel.removeAll()
        
        val filteredTools = if (filterText.isEmpty()) {
            tools
        } else {
            tools.filter { 
                it.name.lowercase().contains(filterText) || 
                it.description?.lowercase()?.contains(filterText) == true
            }
        }
        
        val parentPanel = toolsPanel.parent
        if (filteredTools.isEmpty() && filterText.isNotEmpty()) {
            parentPanel.isVisible = false
        } else {
            parentPanel.isVisible = true
            
            if (filteredTools.isEmpty()) {
                val noToolsLabel = JBLabel("No tools available for $serverName").apply {
                    foreground = textGray
                    horizontalAlignment = SwingConstants.LEFT
                }
                toolsPanel.add(noToolsLabel)
            } else {
                filteredTools.forEach { tool ->
                    val panel = McpToolListCardPanel(project, serverName, tool)
                    toolsPanel.add(panel)
                }
            }
        }
        
        toolsPanel.revalidate()
        toolsPanel.repaint()
        getContentPanel().revalidate()
        getContentPanel().repaint()
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
        getContentPanel().removeAll()
        
        val noServersPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(16)
        }
        
        val noServersLabel = JBLabel("No MCP servers configured. Please check your configuration.").apply {
            foreground = textGray
            horizontalAlignment = SwingConstants.CENTER
        }
        
        noServersPanel.add(noServersLabel, BorderLayout.CENTER)
        getContentPanel().add(noServersPanel)
        getContentPanel().revalidate()
        getContentPanel().repaint()
    }
    
    fun dispose() {
        loadingJob?.cancel()
    }
    
    fun getTools(): Map<String, List<Tool>> = allTools
}
