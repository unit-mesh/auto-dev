package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.settings.customize.customizeSetting
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import com.intellij.openapi.application.invokeLater
import io.modelcontextprotocol.kotlin.sdk.Tool
import java.awt.Component
import java.awt.Font

class McpConfigPopup {
    companion object {
        fun show(component: JComponent?, project: Project) {
            val configService = project.getService(McpConfigService::class.java)
            val popup = McpConfigPopup()
            popup.createAndShow(component, project, configService)
        }
    }
    
    private val toolsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    private val toolCheckboxMap = mutableMapOf<Pair<String, String>, JCheckBox>() // Stores serverName/toolName to JCheckBox

    private fun createAndShow(component: JComponent?, project: Project, configService: McpConfigService) {
        val mainPanel = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(400, 500)
            border = JBUI.Borders.empty(8)
        }
        
        // Search field
        val searchField = JBTextField().apply {
            emptyText.text = "Search tools..."
            border = JBUI.Borders.empty(4)
        }
        
        // Panel for tool selection (replacing CheckboxTree)
        // toolsPanel is now a class member, initialized above

        val loadingPanel = JBLoadingPanel(BorderLayout(), project)
        loadingPanel.add(JBScrollPane(toolsPanel), BorderLayout.CENTER) // toolsPanel instead of tree
        loadingPanel.preferredSize = Dimension(380, 350)
        
        var currentPopup: com.intellij.openapi.ui.popup.JBPopup? = null
        
        // Button panel
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            
            val refreshButton = JButton("Refresh").apply {
                addActionListener {
                    loadingPanel.startLoading()
                    refreshToolsList(project, configService, loadingPanel)
                }
            }
            add(refreshButton)
            add(Box.createHorizontalGlue())
            
            val applyButton = JButton("Apply").apply {
                addActionListener {
                    saveSelectedTools(configService)
                    currentPopup?.cancel()
                }
            }
            
            val cancelButton = JButton("Cancel").apply {
                addActionListener {
                    currentPopup?.cancel()
                }
            }
            
            add(cancelButton)
            add(Box.createHorizontalStrut(8))
            add(applyButton)
        }
        
        mainPanel.add(searchField, BorderLayout.NORTH)
        mainPanel.add(loadingPanel, BorderLayout.CENTER)
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        // Load tools asynchronously
        loadingPanel.startLoading()
        loadToolsIntoPanel(project, configService, loadingPanel)
        
        // Search functionality
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                filterToolsList(searchField.text)
            }
        })
        
        currentPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(mainPanel, searchField)
            .setTitle("Configure MCP Tools")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setCancelOnClickOutside(false)
            .createPopup()
        
        if (component != null) {
            currentPopup.showUnderneathOf(component)
        } else {
            currentPopup.showCenteredInCurrentWindow(project)
        }
    }
    
    private fun refreshToolsList(
        project: Project,
        configService: McpConfigService,
        loadingPanel: JBLoadingPanel
    ) {
        toolsPanel.removeAll()
        toolCheckboxMap.clear()

        loadToolsIntoPanel(project, configService, loadingPanel)
    }

    private fun loadToolsIntoPanel(
        project: Project,
        configService: McpConfigService,
        loadingPanel: JBLoadingPanel
    ) {
        toolsPanel.removeAll() // Clear previous content
        toolCheckboxMap.clear()

        val loadingLabel = JLabel("Loading tools...")
        loadingLabel.alignmentX = Component.LEFT_ALIGNMENT
        toolsPanel.add(loadingLabel)
        toolsPanel.revalidate()
        toolsPanel.repaint()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mcpServerConfig = project.customizeSetting.mcpServerConfig
                val allTools = configService.getAllAvailableTools(mcpServerConfig)
                val selectedTools = configService.getSelectedTools()

                invokeLater {
                    toolsPanel.removeAll() // Remove "Loading tools..." label

                    if (allTools.isEmpty()) {
                        val noToolsLabel = JLabel("No tools available.")
                        noToolsLabel.alignmentX = Component.LEFT_ALIGNMENT
                        toolsPanel.add(noToolsLabel)
                    } else {
                        allTools.forEach { (serverName, tools) ->
                            val serverLabel = JLabel(serverName).apply {
                                font = font.deriveFont(Font.BOLD)
                                border = JBUI.Borders.emptyTop(8)
                                alignmentX = Component.LEFT_ALIGNMENT
                            }
                            toolsPanel.add(serverLabel)

                            if (tools.isEmpty()) {
                                val noToolsForServerLabel = JLabel("  No tools from this server.").apply {
                                    alignmentX = Component.LEFT_ALIGNMENT
                                }
                                toolsPanel.add(noToolsForServerLabel)
                            } else {
                                tools.forEach { tool ->
                                    val checkBoxText = tool.description?.let { desc ->
                                        if (desc.isNotEmpty()) "${tool.name} - $desc" else tool.name
                                    } ?: tool.name
                                    val checkBox = JCheckBox(checkBoxText).apply {
                                        isSelected = selectedTools[serverName]?.contains(tool.name) == true
                                        alignmentX = Component.LEFT_ALIGNMENT
                                        border = JBUI.Borders.emptyLeft(10)
                                    }
                                    toolCheckboxMap[Pair(serverName, tool.name)] = checkBox
                                    toolsPanel.add(checkBox)
                                }
                            }
                        }
                    }
                    
                    loadingPanel.stopLoading()

                    toolsPanel.revalidate()
                    toolsPanel.repaint()
                    loadingPanel.revalidate() 
                    loadingPanel.repaint()
                    (loadingPanel.parent as? JComponent)?.revalidate()
                    (loadingPanel.parent as? JComponent)?.repaint()
                }
            } catch (e: Exception) {
                invokeLater {
                    toolsPanel.removeAll()
                    val errorLabel = JLabel("Error loading tools: ${e.message}").apply {
                        alignmentX = Component.LEFT_ALIGNMENT
                    }
                    toolsPanel.add(errorLabel)
                    
                    loadingPanel.stopLoading()

                    toolsPanel.revalidate()
                    toolsPanel.repaint()
                    loadingPanel.revalidate()
                    loadingPanel.repaint()
                    (loadingPanel.parent as? JComponent)?.revalidate()
                    (loadingPanel.parent as? JComponent)?.repaint()
                }
            }
        }
    }
    
    private fun saveSelectedTools(configService: McpConfigService) {
        val selectedTools = mutableMapOf<String, MutableSet<String>>()
        
        toolCheckboxMap.forEach { (key, checkBox) ->
            val (serverName, toolName) = key
            if (checkBox.isSelected) {
                selectedTools.computeIfAbsent(serverName) { mutableSetOf() }
                    .add(toolName)
            }
        }
        
        configService.setSelectedTools(selectedTools)
    }
    
    private fun filterToolsList(searchText: String) {
        val lowerSearchText = searchText.lowercase().trim()
        var firstVisible: Component? = null

        toolsPanel.components.forEach { component ->
            when (component) {
                is JCheckBox -> {
                    val toolName = toolCheckboxMap.entries.find { it.value == component }?.key?.second ?: ""
                    val toolDescription = component.text.substringAfter("$toolName - ", "").substringBeforeLast(" - $toolName", "")

                    val isVisible = toolName.lowercase().contains(lowerSearchText) ||
                                    toolDescription.lowercase().contains(lowerSearchText) ||
                                    lowerSearchText.isEmpty()
                    component.isVisible = isVisible
                    if (isVisible && firstVisible == null) {
                        firstVisible = component
                    }
                }
                is JLabel -> {
                    // Server labels or status labels. For now, keep them visible or hide if all children are hidden.
                    // This part can be enhanced to hide server labels if all its tools are hidden.
                    // For simplicity, we'll keep them visible. If search is empty, all are visible.
                    component.isVisible = true
                }
            }
        }
        // If there's a search term and some items are visible, try to scroll to the first visible item.
        if (lowerSearchText.isNotEmpty() && firstVisible != null) {
            val finalFirstVisible = firstVisible
            SwingUtilities.invokeLater {
                toolsPanel.scrollRectToVisible(finalFirstVisible.bounds)
            }
        }

        toolsPanel.revalidate()
        toolsPanel.repaint()
    }
}
