package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.agent.model.CustomAgentConfig
import cc.unitmesh.devti.language.DevInIcons
import cc.unitmesh.devti.language.dataprovider.BuiltinCommand
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

enum class ToolHub(val summaryName: String, val type: String, val description: String) {
    AGENT("Agent", CustomAgentConfig::class.simpleName.toString(), "DevIns all agent for AI Agent to call"),
    COMMAND("Command", BuiltinCommand::class.simpleName.toString(), "DevIns all commands for AI Agent to call"),

    ;

    companion object {
        fun all(): List<ToolHub> {
            return values().toList()
        }
    }
}

class AgentToolOverviewCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        ToolHub.all().forEach { toolHub ->
            val elements = LookupElementBuilder.create(toolHub.summaryName)
                .withIcon(DevInIcons.DEFAULT)
                .withTypeText(toolHub.type, true)
                .withPresentableText(toolHub.summaryName)
                .withTailText(toolHub.description, true)
            result.addElement(PrioritizedLookupElement.withPriority(elements, 0.0))
        }
    }

}
