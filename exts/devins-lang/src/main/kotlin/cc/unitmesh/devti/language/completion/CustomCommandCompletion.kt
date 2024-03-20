package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.language.dataprovider.CustomCommand
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class CustomCommandCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.originalFile.project ?: return
        CustomCommand.all(project).forEach {
            result.addElement(
                LookupElementBuilder.create(it.commandName)
                    .withIcon(it.icon)
                    .withTypeText(it.content, true)
            )
        }
    }
}
