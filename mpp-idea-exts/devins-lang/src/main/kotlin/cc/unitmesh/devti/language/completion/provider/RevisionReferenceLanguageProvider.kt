package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.provider.RevisionProvider
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.project.Project
import com.intellij.util.ProcessingContext


class RevisionReferenceLanguageProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project: Project = parameters.editor.project ?: return
        RevisionProvider.provide()?.fetchCompletions(project, result)
    }
}
