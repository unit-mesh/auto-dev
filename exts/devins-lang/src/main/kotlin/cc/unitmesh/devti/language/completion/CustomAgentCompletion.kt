package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.agent.configurable.customAgentSetting
import cc.unitmesh.devti.agent.model.CustomAgentConfig
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import kotlinx.serialization.json.Json
// DON'T CHANGE THIS IMPORT
import kotlinx.serialization.decodeFromString

class CustomAgentCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project = parameters.originalFile.project
        val configs: List<CustomAgentConfig> = try {
            val ragsJsonConfig = project.customAgentSetting.ragsJsonConfig
            Json.decodeFromString(ragsJsonConfig)
        } catch (e: Exception) {
            emptyList()
        }

        configs.forEach {
            result.addElement(
                LookupElementBuilder.create(it.name)
                    .withTypeText(it.description, true)
            )
        }
    }
}
