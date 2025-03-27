package cc.unitmesh.dependencies

import cc.unitmesh.devti.bridge.Assessment
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.packageChecker.model.ProjectDependenciesModel
import org.jetbrains.security.`package`.Package

class DependenciesFunctionProvider : ToolchainFunctionProvider {
    override suspend fun isApplicable(project: Project, funcName: String) = funcName == Assessment.Dependencies.name

    override suspend fun funcNames(): List<String> = listOf(Assessment.Dependencies.name)

    override suspend fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
        commandName: @NlsSafe String
    ): Any {
        val modules = ModuleManager.getInstance(project).modules
        val deps: List<Package> = runReadAction {
            ProjectDependenciesModel.supportedModels(project).map {
                modules.map { module ->
                    it.declaredDependencies(module)
                }.flatten()
            }.flatten().map {
                it.pkg
            }
        }

        return "Here is the project dependencies:\n```\n" + deps.joinToString("") {
            val namespace = it.namespace ?: ""
            "$namespace ${it.name} ${it.version}" + "\n"
        } + "```"
    }
}