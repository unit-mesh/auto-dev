package cc.unitmesh.devti.language.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class FileReferenceLanguageProvider : CompletionProvider<CompletionParameters>() {
    companion object {
        const val REF_TYPE = "file"
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        // sample file: "file1", "file2"
        listOf("file1", "file2").forEach {
            result.addElement(
                LookupElementBuilder.create(it)
                    .withTypeText("file", true)
            )
        }
    }

}
