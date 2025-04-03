package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.completion.AutoDevFileLookupElement
import cc.unitmesh.devti.sketch.rule.ProjectRule
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ProcessingContext

class RuleCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project = parameters.position.project
        val projectRule = ProjectRule(project)
        val files = projectRule.getAllRules()

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
