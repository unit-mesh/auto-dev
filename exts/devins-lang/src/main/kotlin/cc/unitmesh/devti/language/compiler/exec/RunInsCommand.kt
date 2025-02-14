package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.ProjectRunService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

/**
 * The `RunAutoCommand` class is responsible for executing an auto command on a given project.
 *
 * @property myProject The project to execute the auto command on.
 * @property argument The name of the file to find and run tests for.
 *
 */
class RunInsCommand(val myProject: Project, private val argument: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.RUN

    override suspend fun execute(): String? {
        val task = ProjectRunService.all().mapNotNull { projectRun ->
            val hasTasks = projectRun.tasks(myProject).any { task -> task.contains(argument) }
            if (hasTasks) projectRun else null
        }

        if (task.isNotEmpty()) {
            task.first().run(myProject, argument)
            return "Task run successfully: $argument"
        }


        val virtualFile = myProject.lookupFile(argument.trim()) ?: return "$DEVINS_ERROR: File not found: $argument"
        try {
            val psiFile: PsiFile =
                PsiManager.getInstance(myProject).findFile(virtualFile) ?: return "$DEVINS_ERROR: File not found: $argument"
            val testService =
                AutoTestService.context(psiFile) ?: return "$DEVINS_ERROR: No test service found for file: $argument"
            testService.runFile(myProject, virtualFile, null, true)

            return "Running tests for file: $argument"
        } catch (e: Exception) {
            return "$DEVINS_ERROR: ${e.message}"
        }
    }
}
