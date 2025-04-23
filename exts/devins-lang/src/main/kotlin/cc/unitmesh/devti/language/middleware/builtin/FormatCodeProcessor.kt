package cc.unitmesh.devti.language.middleware.builtin

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import cc.unitmesh.devti.language.middleware.post.PostProcessorType
import cc.unitmesh.devti.language.middleware.post.PostProcessorContext
import cc.unitmesh.devti.language.middleware.post.PostProcessor
import cc.unitmesh.devti.util.workerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FormatCodeProcessor : PostProcessor {
    override val processorName: String = PostProcessorType.FormatCode.handleName
    override val description: String = "`formatCode` will format the code of the current file"

    override fun isApplicable(context: PostProcessorContext): Boolean = true

    override fun execute(project: Project, context: PostProcessorContext, console: ConsoleView?, args: List<Any>): Any {
        val file = context.currentFile ?: return ""
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return ""

        CoroutineScope(workerThread).launch {
            WriteCommandAction.runWriteCommandAction(project) {
                val codeStyleManager = CodeStyleManager.getInstance(project)
                if (context.modifiedTextRange != null) {
                    codeStyleManager.reformatText(file, listOf(context.modifiedTextRange))
                } else if (context.genPsiElement != null) {
                    codeStyleManager.reformat(context.genPsiElement!!)
                } else {
                    codeStyleManager.reformatText(file, 0, document.textLength)
                }
            }
        }

        return context.genText ?: ""
    }
}
