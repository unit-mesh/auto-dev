package cc.unitmesh.dependencies

import cc.unitmesh.devti.bridge.Assessment
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.packageChecker.model.ProjectDependenciesModel
import org.jetbrains.security.`package`.Package
import java.util.concurrent.CompletableFuture

class DependenciesFunctionProvider : ToolchainFunctionProvider {
    override fun isApplicable(project: Project, funcName: String) = funcName == Assessment.Dependencies.name

    override fun funcNames(): List<String> = listOf(Assessment.Dependencies.name)

    override fun execute(
        project: Project,
        prop: String,
        args: List<Any>,
        allVariables: Map<String, Any?>,
        commandName: @NlsSafe String
    ): Any {
        val modules = ModuleManager.getInstance(project).modules
        val future = CompletableFuture<String>()
        val task = object : Task.Backgroundable(project, "Processing context", false) {
            override fun run(indicator: ProgressIndicator) {
               try {
                   val deps: List<Package> = runReadAction {
                       ProjectDependenciesModel.supportedModels(project).map {
                           modules.map { module ->
                               it.declaredDependencies(module)
                           }.flatten()
                       }.flatten().map {
                           it.pkg
                       }
                   }

                   val result = "Here is the project dependencies:\n```\n" + deps.joinToString("") {
                       val namespace = it.namespace ?: ""
                       "$namespace ${it.name} ${it.version}" + "\n"
                   } + "```"

                   future.complete(result)
               } catch (e: Exception) {
                   future.completeExceptionally(e)
               }
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

        return future.get(30, java.util.concurrent.TimeUnit.SECONDS)
    }
}