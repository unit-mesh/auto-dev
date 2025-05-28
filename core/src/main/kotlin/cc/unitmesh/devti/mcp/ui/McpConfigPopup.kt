package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.settings.customize.customizeSetting
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleTextAttributes
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
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import com.intellij.openapi.application.invokeLater
import io.modelcontextprotocol.kotlin.sdk.Tool

class ServerTreeNode(val serverName: String) : CheckedTreeNode(serverName) {
    init {
        allowsChildren = true
    }
}

class ToolTreeNode(val serverName: String, val tool: Tool) : CheckedTreeNode(tool.name) {
    init {
        allowsChildren = false
        userObject = tool.name
    }

    override fun toString(): String = tool.name
}


class McpConfigPopup {
    companion object {
        fun show(component: JComponent?, project: Project) {
            val configService = project.getService(McpConfigService::class.java)
            val popup = McpConfigPopup()
            popup.createAndShow(component, project, configService)
        }
    }
    
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
        
        // Tree for tool selection
        val rootNode = CheckedTreeNode("MCP Tools")
        val treeModel = DefaultTreeModel(rootNode)
        val tree = CheckboxTree(object : CheckboxTree.CheckboxTreeCellRenderer() {
            override fun customizeRenderer(
                tree: JTree?,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ) {
                if (value is ToolTreeNode) {
                    textRenderer.append(value.tool.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    value.tool.description?.let { desc ->
                        if (desc.isNotEmpty()) {
                            textRenderer.append(" - $desc", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                        }
                    }
                } else if (value is ServerTreeNode) {
                    textRenderer.append(value.serverName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                } else {
                    textRenderer.append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
        }, rootNode).apply {
            isRootVisible = false
            showsRootHandles = true
        }
        
        val loadingPanel = JBLoadingPanel(BorderLayout(), project)
        loadingPanel.add(JBScrollPane(tree), BorderLayout.CENTER)
        loadingPanel.preferredSize = Dimension(380, 350)
        
        var currentPopup: com.intellij.openapi.ui.popup.JBPopup? = null
        
        // Button panel
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            
            val refreshButton = JButton("Refresh").apply {
                addActionListener {
                    loadingPanel.startLoading()
                    refreshToolsList(project, configService, rootNode, treeModel, tree, loadingPanel)
                }
            }
            add(refreshButton)
            add(Box.createHorizontalGlue())
            
            val applyButton = JButton("Apply").apply {
                addActionListener {
                    saveSelectedTools(tree, configService)
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
        loadToolsIntoTree(project, configService, rootNode, treeModel, tree, loadingPanel)
        
        // Search functionality
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                filterTree(tree, rootNode, searchField.text)
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
        rootNode: CheckedTreeNode,
        treeModel: DefaultTreeModel,
        tree: CheckboxTree,
        loadingPanel: JBLoadingPanel
    ) {
        rootNode.removeAllChildren()
        treeModel.reload()
        
        loadToolsIntoTree(project, configService, rootNode, treeModel, tree, loadingPanel)
    }

    private fun loadToolsIntoTree(
        project: Project,
        configService: McpConfigService,
        rootNode: CheckedTreeNode,
        treeModel: DefaultTreeModel,
        tree: CheckboxTree,
        loadingPanel: JBLoadingPanel
    ) {
        rootNode.removeAllChildren()
        val loadingNode = CheckedTreeNode("Loading tools...")
        rootNode.add(loadingNode)
        treeModel.reload(rootNode) // Reload to show the loading node
        expandAllNodes(tree)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mcpServerConfig = project.customizeSetting.mcpServerConfig
                val allTools = configService.getAllAvailableTools(mcpServerConfig)
                val selectedTools = configService.getSelectedTools()

                invokeLater {
                    rootNode.removeAllChildren() // Remove "Loading tools..." node

                    if (allTools.isEmpty()) {
                        val noToolsNode = CheckedTreeNode("No tools available.")
                        rootNode.add(noToolsNode)
                    } else {
                        allTools.forEach { (serverName, tools) ->
                            val serverNode = ServerTreeNode(serverName)
                            rootNode.add(serverNode)

                            if (tools.isEmpty()) {
                                val noToolsForServerNode = CheckedTreeNode("No tools from this server.")
                                serverNode.add(noToolsForServerNode)
                            } else {
                                tools.forEach { tool ->
                                    val toolNode = ToolTreeNode(serverName, tool)
                                    val isSelected = selectedTools[serverName]?.contains(tool.name) == true
                                    toolNode.isChecked = isSelected
                                    serverNode.add(toolNode)
                                }
                            }
                        }
                    }

                    treeModel.nodeStructureChanged(rootNode) // Notify that rootNode's children changed
                    expandAllNodes(tree)
                    
                    loadingPanel.stopLoading()

                    tree.revalidate() // Ensure tree layout is updated
                    tree.repaint() 
                    loadingPanel.revalidate() 
                    loadingPanel.repaint()
                    // Also revalidate and repaint parent in case its layout depends on loadingPanel
                    (loadingPanel.parent as? JComponent)?.revalidate()
                    (loadingPanel.parent as? JComponent)?.repaint()
                }
            } catch (e: Exception) {
                invokeLater {
                    rootNode.removeAllChildren()
                    val errorNode = CheckedTreeNode("Error loading tools: ${e.message}")
                    rootNode.add(errorNode)
                    treeModel.nodeStructureChanged(rootNode) // Notify change
                    expandAllNodes(tree)

                    loadingPanel.stopLoading()

                    tree.revalidate()
                    tree.repaint()
                    loadingPanel.revalidate()
                    loadingPanel.repaint()
                    (loadingPanel.parent as? JComponent)?.revalidate()
                    (loadingPanel.parent as? JComponent)?.repaint()
                }
            }
        }
    }
    
    private fun saveSelectedTools(tree: CheckboxTree, configService: McpConfigService) {
        val selectedTools = mutableMapOf<String, MutableSet<String>>()
        
        val root = tree.model.root as CheckedTreeNode
        for (i in 0 until root.childCount) {
            val serverNode = root.getChildAt(i) as ServerTreeNode
            val serverName = serverNode.serverName
            
            for (j in 0 until serverNode.childCount) {
                val toolNode = serverNode.getChildAt(j) as ToolTreeNode
                if (toolNode.isChecked) {
                    selectedTools.computeIfAbsent(serverName) { mutableSetOf() }
                        .add(toolNode.tool.name)
                }
            }
        }
        
        configService.setSelectedTools(selectedTools)
    }
    
    private fun filterTree(tree: CheckboxTree, rootNode: CheckedTreeNode, searchText: String) {
        tree.expandPath(TreePath(rootNode.path))
    }
    
    private fun expandAllNodes(tree: CheckboxTree) {
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
    }
}
