package cc.unitmesh.dependencies

import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.packageChecker.api.BuildFileProvider
import com.intellij.packageChecker.api.PackageDeclaration
import com.intellij.packageChecker.model.ProjectDependenciesModel

class DependenciesFunctionProvider : ToolchainFunctionProvider {
    override fun isApplicable(project: Project, funcName: String): Boolean {
        return funcName == "dependencies"
    }

    fun listDeps(project: Project): List<PackageDeclaration> {
        val modules = ModuleManager.getInstance(project).modules
        val flatten = ProjectDependenciesModel.supportedModels(project).map {
            modules.map { module ->
                it.declaredDependencies(module)
            }.flatten()
        }.flatten()

//        BuildFileProvider.EP_NAME
        return flatten
    }

    override fun execute(
        project: Project,
        funcName: String,
        args: List<Any>,
        allVariables: Map<String, Any?>
    ): Any {
        return listDeps(project)
    }
}