package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.agent.model.CustomAgentConfig
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class CustomAgentCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val configs: List<CustomAgentConfig> = CustomAgentConfig.loadFromProject(parameters.originalFile.project)

        configs.forEach {
            result.addElement(
                LookupElementBuilder.create(it.name)
                    .withTypeText(it.description, true)
            )
        }
    }
}
