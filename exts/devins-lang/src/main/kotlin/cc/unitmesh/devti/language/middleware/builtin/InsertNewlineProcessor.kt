package cc.unitmesh.devti.language.middleware.builtin

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import cc.unitmesh.devti.language.middleware.post.PostProcessorType
import cc.unitmesh.devti.language.middleware.post.PostProcessorContext
import cc.unitmesh.devti.language.middleware.post.PostProcessor
import cc.unitmesh.devti.util.workerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class InsertNewlineProcessor : PostProcessor {
    override val processorName: String = PostProcessorType.InsertNewline.handleName
    override val description: String = "`insertNewline` will insert a newline at the cursor position"

    override fun isApplicable(context: PostProcessorContext): Boolean = true

    override fun execute(project: Project, context: PostProcessorContext, console: ConsoleView?, args: List<Any>): Any {
        val editor = context.editor ?: return ""

        CoroutineScope(workerThread).launch {
            WriteCommandAction.runWriteCommandAction(project) {
                editor.document.insertString(editor.caretModel.offset, "\n")
            }
        }

        return editor.document.text
    }
}
