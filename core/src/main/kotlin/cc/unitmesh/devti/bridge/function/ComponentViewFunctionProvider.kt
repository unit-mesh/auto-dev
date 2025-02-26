package cc.unitmesh.devti.bridge.function

import cc.unitmesh.devti.bridge.ArchViewCommand
import cc.unitmesh.devti.bridge.provider.UiComponentProvider
import cc.unitmesh.devti.bridge.tools.UiComponent
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.project.Project

class ComponentViewFunctionProvider : ToolchainFunctionProvider {
    override fun isApplicable(project: Project, funcName: String) = funcName == ArchViewCommand.ComponentView.name

    override fun execute(
        project: Project,
        funcName: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): String = UiComponentProvider.Companion.collect(project).joinToString("\n", transform = UiComponent::format)
}