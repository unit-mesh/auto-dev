package cc.unitmesh.devti.bridge

import cc.unitmesh.devti.bridge.provider.UiComponentProvider
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.project.Project

class ComponentViewFunctionProvider : ToolchainFunctionProvider {
    override fun isApplicable(project: Project, funcName: String): Boolean {
        return funcName == ArchViewCommand.ComponentView.name
    }

    override fun execute(
        project: Project,
        funcName: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): Any {
        return UiComponentProvider.collect(project).joinToString("\n") {
            it.format()
        }
    }
}
