package cc.unitmesh.devti.bridge.archview

import cc.unitmesh.devti.bridge.ArchViewCommand
import cc.unitmesh.devti.bridge.provider.ComponentViewProvider
import cc.unitmesh.devti.bridge.archview.model.UiComponent
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.project.Project

class ComponentViewFunctionProvider : ToolchainFunctionProvider {
    override fun isApplicable(project: Project, funcName: String) = funcName == ArchViewCommand.ComponentView.name

    override fun execute(
        project: Project,
        funcName: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): String = ComponentViewProvider.collect(project).joinToString("\n", transform = UiComponent::format)
}