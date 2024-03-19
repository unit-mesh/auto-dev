package cc.unitmesh.devti.language

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.util.ProcessingContext
import javax.swing.Icon

enum class FileFunc(val funcName: String, val description: String, val icon: Icon) {
    Regex("regex", "Read the content of a file by regex", AllIcons.Actions.Regex),

    ;

    companion object {
        fun all(): List<FileFunc> {
            return values().toList()
        }

        fun fromString(funcName: String): FileFunc? {
            return values().find { it.funcName == funcName }
        }
    }
}

class FileFunctionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        FileFunc.all().forEach {
            result.addElement(
                LookupElementBuilder.create(it.funcName)
                    .withIcon(it.icon)
                    .withTypeText(it.description, true)
            )
        }
    }
}
