package cc.unitmesh.devti.language.middleware.builtin

import cc.unitmesh.devti.language.middleware.builtin.psi.PsiErrorCollector
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import cc.unitmesh.devti.language.middleware.post.PostProcessor
import cc.unitmesh.devti.language.middleware.post.PostProcessorContext
import cc.unitmesh.devti.language.middleware.post.PostProcessorType

class VerifyCodeProcessor : PostProcessor {
    override val processorName: String = PostProcessorType.VerifyCode.handleName
    override val description: String = "`verifyCode` will verify the code syntax and return the errors"

    override fun isApplicable(context: PostProcessorContext): Boolean = true

    override fun execute(
        project: Project,
        context: PostProcessorContext,
        console: ConsoleView?,
        args: List<Any>,
    ): String {
        val code = context.pipeData["output"]
        if (code !is VirtualFile) {
            console?.print("No code to verify\n", ConsoleViewContentType.ERROR_OUTPUT)
            return ""
        }

        val psiFile = PsiManager.getInstance(project).findFile(code)
        if (psiFile == null) {
            console?.print("No code to verify\n", ConsoleViewContentType.ERROR_OUTPUT)
            return ""
        }

        if (!psiFile.isValid) {
            console?.print("No code to verify\n", ConsoleViewContentType.ERROR_OUTPUT)
            return ""
        }

        val errors: List<String> = PsiErrorCollector.collectSyntaxError(psiFile, project)

        if (errors.isNotEmpty()) {
            console?.print("Syntax errors found:\n${errors.joinToString("\n")}\n", ConsoleViewContentType.ERROR_OUTPUT)
        } else {
            console?.print("No syntax errors found\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        }

        return errors.joinToString("\n")
    }
}
