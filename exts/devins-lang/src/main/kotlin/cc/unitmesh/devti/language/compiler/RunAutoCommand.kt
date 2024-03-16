package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.compiler.utils.lookupFile
import cc.unitmesh.devti.provider.AutoTestService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class RunAutoCommand(val myProject: Project, val prop: String) : AutoCommand {
    override fun execute(): String? {
        val virtualFile = myProject.lookupFile(prop.trim()) ?: return "<DevliError>: File not found: $prop"
        try {
            val psiFile: PsiFile =
                PsiManager.getInstance(myProject).findFile(virtualFile) ?: return "<DevliError>: File not found: $prop"
            val testService =
                AutoTestService.context(psiFile) ?: return "<DevliError>: No test service found for file: $prop"
            testService.runFile(myProject, virtualFile)

            return "Running tests for file: $prop"
        } catch (e: Exception) {
            return "<DevliError>: ${e.message}"
        }
    }
}
