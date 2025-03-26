package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.gui.chat.AutoDevFileLookupElement
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.util.ProcessingContext

class RuleCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project = parameters.position.project

       val rulePath = "prompts/rules"
        val ruleDir = project.guessProjectDir()?.findFileByRelativePath(rulePath) ?: return
        val files = ruleDir.children.filter { it.isFile && (it.extension == "md") }

        files.forEach { file ->
            val priority = 1.0
            result.addElement(buildElement(file, priority))
        }
    }

    private fun buildElement(virtualFile: VirtualFile, priority: Double): LookupElement {
        val filename = virtualFile.nameWithoutExtension

        val elementBuilder = LookupElementBuilder.create(filename)
            .withInsertHandler { context, _ ->
                context.editor.caretModel.moveCaretRelatively(1, 1, false, false, false)
            }

        return AutoDevFileLookupElement.withPriority(elementBuilder, priority, virtualFile)
    }
}

