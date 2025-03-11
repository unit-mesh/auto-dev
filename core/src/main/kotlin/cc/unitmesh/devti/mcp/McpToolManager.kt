package cc.unitmesh.devti.mcp

import com.intellij.openapi.extensions.ExtensionPointName

class McpToolManager {
    companion object {
        private val EP_NAME = ExtensionPointName<AbstractMcpTool<*>>("cc.unitmesh.mcpTool")

        fun getAllTools(): List<AbstractMcpTool<*>> {
            return buildList {
                // Add built-in tools
                addAll(getBuiltInTools())
                // Add extension-provided tools
                addAll(EP_NAME.extensionList)
            }
        }

        private fun getBuiltInTools(): List<AbstractMcpTool<*>> = listOf(
            GetCurrentFileTextTool(),
            GetCurrentFilePathTool(),
            GetSelectedTextTool(),
            ReplaceSelectedTextTool(),
            ReplaceCurrentFileTextTool(),
            CreateNewFileWithTextTool(),
            FindFilesByNameSubstring(),
            GetFileTextByPathTool(),
            ReplaceTextByPathTool(),
            ListFilesInFolderTool(),
            SearchInFilesContentTool(),
            RunConfigurationTool(),
            GetRunConfigurationsTool(),
            GetProjectModulesTool(),
            GetProjectDependenciesTool(),
            GetAllOpenFileTextsTool(),
            GetAllOpenFilePathsTool(),
            OpenFileInEditorTool(),
            ListAvailableActionsTool(),
            ExecuteActionByIdTool(),
            GetProgressIndicatorsTool(),
            WaitTool(),
        )
    }
}