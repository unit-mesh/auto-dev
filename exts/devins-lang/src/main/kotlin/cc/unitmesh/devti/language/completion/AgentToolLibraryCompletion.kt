package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.language.DevInIcons
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class AgentToolLibraryCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val command = PrioritizedLookupElement.withPriority(
            LookupElementBuilder.create("commands")
                .withIcon(DevInIcons.DEFAULT)
                .withTypeText("DevIns all commands for AI Agent to call", true),
            0.0
        )

        val agent = PrioritizedLookupElement.withPriority(
            LookupElementBuilder.create("agents")
                .withIcon(DevInIcons.DEFAULT)
                .withTypeText("DevIns all agent for AI Agent to call", true),
            0.0
        )

        result.addElement(command)
        result.addElement(agent)
    }

}
