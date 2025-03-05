package cc.unitmesh.devti.bridge.archview

import cc.unitmesh.devti.bridge.ArchViewCommand
import cc.unitmesh.devti.bridge.provider.ComponentViewProvider
import cc.unitmesh.devti.bridge.archview.model.UiComponent
import cc.unitmesh.devti.bridge.provider.ComponentViewMode
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.project.Project

class ComponentViewFunctionProvider : ToolchainFunctionProvider {
    override fun funcNames(): List<String> = listOf(ArchViewCommand.ComponentView.name)

    override fun isApplicable(project: Project, funcName: String) = funcName == ArchViewCommand.ComponentView.name

    override fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): String {
        val uiComponents = ComponentViewProvider.collect(project, ComponentViewMode.DEFAULT)
        val transform = if (prop == "all") {
            UiComponent::format
        } else {
            UiComponent::simple
        }

        val components = uiComponents.joinToString("\n", transform = transform)
        return "Here is current project ${uiComponents.size} components, \n$components"
    }
}