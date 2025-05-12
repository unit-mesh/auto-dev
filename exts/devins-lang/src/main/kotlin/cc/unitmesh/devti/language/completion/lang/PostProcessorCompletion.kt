package cc.unitmesh.devti.language.completion.lang

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.devins.post.PostProcessor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class PostProcessorCompletion : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        PostProcessor.allNames().forEach {
            result.addElement(
                LookupElementBuilder
                    .create(it)
                    .withIcon(AutoDevIcons.AI_COPILOT)
                    .withInsertHandler { context: InsertionContext, item: LookupElement ->

                    }
            )
        }
    }
}