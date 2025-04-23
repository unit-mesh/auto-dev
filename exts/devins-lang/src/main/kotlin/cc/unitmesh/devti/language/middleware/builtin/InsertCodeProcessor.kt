package cc.unitmesh.devti.language.middleware.builtin

import cc.unitmesh.devti.context.builder.CodeModifier
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import cc.unitmesh.devti.language.middleware.post.PostProcessorType
import cc.unitmesh.devti.language.middleware.post.PostProcessorContext
import cc.unitmesh.devti.language.middleware.post.PostProcessor

class InsertCodeProcessor : PostProcessor {
    override val processorName: String = PostProcessorType.InsertCode.handleName
    override val description: String = "`insertCode` will insert the code to the current file"

    override fun isApplicable(context: PostProcessorContext): Boolean = true

    override fun execute(project: Project, context: PostProcessorContext, console: ConsoleView?, args: List<Any>): String {
        if (context.currentLanguage == null || context.currentFile == null) {
            console?.print("No found current language\n", ConsoleViewContentType.ERROR_OUTPUT)
            return ""
        }


        val codeModifier = CodeModifier.forLanguage(context.currentLanguage!!)
        if (codeModifier == null) {
            console?.print("No code modifier found\n", ConsoleViewContentType.NORMAL_OUTPUT)
            // insert to the end of the file
            val editor = context.editor ?: return ""
            WriteCommandAction.runWriteCommandAction(project) {
                editor.document.insertString(editor.caretModel.offset, context.genText ?: "")
            }
            return ""
        }

        context.genPsiElement = codeModifier.smartInsert(context.currentFile!!.virtualFile!!, project, context.genText ?: "")
        return ""
    }
}
