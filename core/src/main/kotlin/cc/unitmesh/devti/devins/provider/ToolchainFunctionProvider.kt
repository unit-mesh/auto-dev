package cc.unitmesh.devti.devins.provider

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface ToolchainFunctionProvider {
    fun isApplicable(project: Project, funcName: String): Boolean

    fun execute(project: Project, funcName: String, args: List<Any>, allVariables: Map<String, Any?>): Any

    companion object {
        private val EP_NAME: ExtensionPointName<ToolchainFunctionProvider> =
            ExtensionPointName("cc.unitmesh.shireToolchainFunctionProvider")

        fun all(): List<ToolchainFunctionProvider> {
            return EP_NAME.extensionList
        }

        fun lookup(providerName: String): ToolchainFunctionProvider? {
            return EP_NAME.extensionList.firstOrNull {
                it.javaClass.simpleName == providerName
            }
        }

        fun provide(project: Project, funcName: String): ToolchainFunctionProvider? {
            return EP_NAME.extensionList.firstOrNull {
                it.isApplicable(project, funcName)
            }
        }
    }
}
