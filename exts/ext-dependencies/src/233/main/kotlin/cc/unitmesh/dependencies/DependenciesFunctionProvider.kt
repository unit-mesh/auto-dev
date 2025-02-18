package cc.unitmesh.dependencies

import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.packageChecker.model.ProjectDependenciesModel

class DependenciesFunctionProvider : ToolchainFunctionProvider {
    override fun isApplicable(project: Project, funcName: String) = funcName == "dependencies"

    override fun execute(
        project: Project,
        funcName: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): Any {
        val modules = ModuleManager.getInstance(project).modules
        return ProjectDependenciesModel.supportedModels(project).map {
            modules.map { module ->
                it.declaredDependencies(module)
            }.flatten()
        }.flatten()
    }
}