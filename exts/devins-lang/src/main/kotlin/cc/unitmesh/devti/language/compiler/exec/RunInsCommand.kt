package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.utils.lookupFile
import cc.unitmesh.devti.provider.AutoTestService
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
    override suspend fun execute(): String? {
        val virtualFile = myProject.lookupFile(argument.trim()) ?: return "<DevInsError>: File not found: $argument"
        try {
            val psiFile: PsiFile =
                PsiManager.getInstance(myProject).findFile(virtualFile) ?: return "<DevInsError>: File not found: $argument"
            val testService =
                AutoTestService.context(psiFile) ?: return "<DevInsError>: No test service found for file: $argument"
            testService.runFile(myProject, virtualFile)

            return "Running tests for file: $argument"
        } catch (e: Exception) {
            return "<DevInsError>: ${e.message}"
        }
    }
}
