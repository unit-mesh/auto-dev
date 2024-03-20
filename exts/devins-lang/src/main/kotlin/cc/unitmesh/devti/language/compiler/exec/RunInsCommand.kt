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
 * @property prop The name of the file to find and run tests for.
 *
 */
class RunInsCommand(val myProject: Project, val prop: String) : InsCommand {
    override fun execute(): String? {
        val virtualFile = myProject.lookupFile(prop.trim()) ?: return "<DevInsError>: File not found: $prop"
        try {
            val psiFile: PsiFile =
                PsiManager.getInstance(myProject).findFile(virtualFile) ?: return "<DevInsError>: File not found: $prop"
            val testService =
                AutoTestService.context(psiFile) ?: return "<DevInsError>: No test service found for file: $prop"
            testService.runFile(myProject, virtualFile)

            return "Running tests for file: $prop"
        } catch (e: Exception) {
            return "<DevInsError>: ${e.message}"
        }
    }
}
