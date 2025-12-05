package cc.unitmesh.devti.mcp.host

import com.intellij.openapi.extensions.ExtensionPointName

class HostMcpToolManager {
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

            /// autodev code
            IssueEvaluateTool(),
            CreateTestForFileTool(),
        )
    }
}