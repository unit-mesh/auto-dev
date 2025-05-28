package cc.unitmesh.devti.mcp.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleTextAttributes
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
        
        val scrollPane = JBScrollPane(tree).apply {
            preferredSize = Dimension(380, 350)
        }
        
        // Button panel
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(Box.createHorizontalGlue())
            
            val applyButton = JButton("Apply").apply {
                addActionListener {
                    saveSelectedTools(tree, configService)
                    // Close popup - this will be handled by the popup framework
                }
            }
            
            val cancelButton = JButton("Cancel")
            
            add(cancelButton)
            add(Box.createHorizontalStrut(8))
            add(applyButton)
        }
        
        mainPanel.add(searchField, BorderLayout.NORTH)
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        // Load tools asynchronously
        loadToolsIntoTree(project, configService, rootNode, treeModel, tree)
        
        // Search functionality
        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                filterTree(tree, rootNode, searchField.text)
            }
        })
        
        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(mainPanel, searchField)
            .setTitle("Configure MCP Tools")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()
        
        if (component != null) {
            popup.showUnderneathOf(component)
        } else {
            popup.showCenteredInCurrentWindow(project)
        }
    }
    
    private fun loadToolsIntoTree(
        project: Project,
        configService: McpConfigService,
        rootNode: CheckedTreeNode,
        treeModel: DefaultTreeModel,
        tree: CheckboxTree
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            // Use empty content for now, or you can pass actual content based on context
            val allTools = configService.getAllAvailableTools("")
            val selectedTools = configService.getSelectedTools()
            
            CoroutineScope(Dispatchers.IO).launch {
                rootNode.removeAllChildren()
                
                allTools.forEach { (serverName, tools) ->
                    val serverNode = ServerTreeNode(serverName)
                    rootNode.add(serverNode)
                    
                    tools.forEach { tool ->
                        val toolNode = ToolTreeNode(serverName, tool)
                        val isSelected = selectedTools[serverName]?.contains(tool.name) == true
                        toolNode.isChecked = isSelected
                        serverNode.add(toolNode)
                    }
                }
                
                treeModel.reload()
                expandAllNodes(tree)
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
        // Simple implementation - in a real scenario, you might want more sophisticated filtering
        tree.expandPath(TreePath(rootNode.path))
    }
    
    private fun expandAllNodes(tree: CheckboxTree) {
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
    }
}
