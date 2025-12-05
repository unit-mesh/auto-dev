package cc.unitmesh.devti.bridge.archview

import cc.unitmesh.devti.bridge.ArchViewCommand
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe

class ContainerViewFunctionProvider : ToolchainFunctionProvider {
    override suspend fun isApplicable(project: Project, funcName: String) =
        funcName == ArchViewCommand.ContainerView.name

    override suspend fun funcNames(): List<String> = listOf(ArchViewCommand.ContainerView.name)

    override suspend fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
        commandName: @NlsSafe String
    ): String {
        val modules = ModuleManager.getInstance(project).modules
        return "Here is current project modules:\n```\n" + modules.joinToString("\n") {
            "module: ${it.getOptionValue("type")} - ${it.name}"
        } + "\n```"
    }
}