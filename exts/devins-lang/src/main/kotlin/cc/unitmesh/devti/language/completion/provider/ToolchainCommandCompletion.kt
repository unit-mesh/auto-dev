package cc.unitmesh.devti.language.completion.provider

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class ToolchainCommandCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        BuiltinCommand.allToolchains().forEach {
            val lookupElement = createCommandCompletionCandidate(it)
            result.addElement(lookupElement)
        }
    }

    private fun createCommandCompletionCandidate(it: String) =
        PrioritizedLookupElement.withPriority(
            LookupElementBuilder.create(it).withIcon(AutoDevIcons.TOOLCHAIN),
            98.0
        )
}

