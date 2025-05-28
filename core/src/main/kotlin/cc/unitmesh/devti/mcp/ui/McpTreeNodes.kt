package cc.unitmesh.devti.mcp.ui

import com.intellij.ui.CheckedTreeNode
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
