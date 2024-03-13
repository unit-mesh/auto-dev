package cc.unitmesh.devti.custom.completion

import cc.unitmesh.devti.agent.model.CustomAgentConfig
import cc.unitmesh.devti.custom.schema.CUSTOM_AGENT_FILE_NAME
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.JsonElementTypes
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

class CustomAgentJsonCompletionContributor : CompletionContributor() {
    internal fun jsonPropertyName() = PlatformPatterns.psiElement(JsonElementTypes.IDENTIFIER)

    internal fun jsonStringValue() =
        PlatformPatterns.psiElement(JsonElementTypes.SINGLE_QUOTED_STRING).withParent<JsonStringLiteral>()

    internal inline fun <reified T : PsiElement> PsiElementPattern<*, *>.withParent() = this.withParent(T::class.java)

    init {
        extend(
            CompletionType.BASIC,
            jsonPropertyName(),
            CustomAgentFieldsProvider()
        )
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.position.containingFile.name != CUSTOM_AGENT_FILE_NAME) {
            return
        }

        super.fillCompletionVariants(parameters, result)
    }
}

class CustomAgentFieldsProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        CustomAgentConfig::class.java.declaredFields.forEach {
            result.addElement(LookupElementBuilder.create("\"" + it.name + "\""))
        }
    }
}
