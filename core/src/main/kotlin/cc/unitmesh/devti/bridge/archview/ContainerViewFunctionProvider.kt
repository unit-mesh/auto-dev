package cc.unitmesh.devti.bridge.archview

import cc.unitmesh.devti.bridge.ArchViewCommand
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project

class ContainerViewFunctionProvider : ToolchainFunctionProvider {
    override fun isApplicable(project: Project, funcName: String) = funcName == ArchViewCommand.ContainerView.name

    override fun funcNames(): List<String> = listOf(ArchViewCommand.ContainerView.name)

    override fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): String {
        val modules = ModuleManager.getInstance(project).modules
        return "Here is current project modules:" + modules.joinToString("\n") {
            "module: ${it.moduleTypeName} - ${it.name}"
        }
    }
}