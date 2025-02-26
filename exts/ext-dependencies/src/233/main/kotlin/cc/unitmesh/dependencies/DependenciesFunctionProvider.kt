package cc.unitmesh.dependencies

import cc.unitmesh.devti.bridge.Assessment
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.packageChecker.model.ProjectDependenciesModel

class DependenciesFunctionProvider : ToolchainFunctionProvider {
    override fun isApplicable(project: Project, funcName: String) = funcName == Assessment.Dependencies.name

    override fun funcNames(): List<String> = listOf(Assessment.Dependencies.name)

    override fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): Any {
        val modules = ModuleManager.getInstance(project).modules
        val deps = ProjectDependenciesModel.supportedModels(project).map {
            modules.map { module ->
                it.declaredDependencies(module)
            }.flatten()
        }.flatten().map {
            it.pkg
        }

        return "```dependencies\n" + deps.joinToString("\n") {
            val namespace = it.namespace ?: ""
            "$namespace ${it.name} ${it.version}" + "\n"
        } + "\n```"
    }
}