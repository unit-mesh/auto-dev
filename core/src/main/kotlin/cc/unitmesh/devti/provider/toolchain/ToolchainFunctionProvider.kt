package cc.unitmesh.devti.provider.toolchain

import cc.unitmesh.devti.agent.tool.AgentTool
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface ToolchainFunctionProvider {
    suspend fun toolInfos(): List<AgentTool> = emptyList()

    suspend fun funcNames(): List<String>

    suspend fun isApplicable(project: Project, funcName: String): Boolean

    suspend fun execute(project: Project, prop: String, args: List<Any>, allVariables: Map<String, Any?>, commandName: String): Any

    companion object {
        private val EP_NAME: ExtensionPointName<ToolchainFunctionProvider> =
            ExtensionPointName("cc.unitmesh.toolchainFunctionProvider")

        fun all(): List<ToolchainFunctionProvider> {
            return EP_NAME.extensionList
        }

        fun lookup(providerName: String): ToolchainFunctionProvider? {
            return EP_NAME.extensionList.firstOrNull {
                it.javaClass.simpleName == providerName
            }
        }
    }
}
