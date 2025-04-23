package cc.unitmesh.devti.language.middleware.builtin

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import cc.unitmesh.devti.language.middleware.post.PostProcessorType
import cc.unitmesh.devti.language.middleware.post.PostProcessorContext
import cc.unitmesh.devti.language.middleware.post.PostProcessor
import cc.unitmesh.devti.util.workerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class UpdateEditorTextProcessor : PostProcessor {
    override val processorName: String = PostProcessorType.UpdateEditorText.handleName
    override val description: String = "`updateEditorText` will update the editor text from llm response"

    override fun isApplicable(context: PostProcessorContext): Boolean = true

    override fun execute(
        project: Project,
        context: PostProcessorContext,
        console: ConsoleView?,
        args: List<Any>,
    ): Any {
        val editor = context.editor ?: return ""
        val newText = if(args.isNotEmpty()) {
            args[0]
        } else {
            context.pipeData["output"]
        }

        if (newText == null) {
            logger<UpdateEditorTextProcessor>().error("no new code to update, pipeData: ${context.pipeData}")
            return ""
        }

        CoroutineScope(workerThread).launch {
            WriteCommandAction.runWriteCommandAction(project) {
                editor.document.setText(newText.toString())
            }
        }

        return newText
    }
}
