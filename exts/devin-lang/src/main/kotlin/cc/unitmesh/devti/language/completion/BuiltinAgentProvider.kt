package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.language.DevInIcons
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

enum class BuiltinAgent(val agentName: String, val description: String) {
    FILE("file", "Read the content of a file"),
    REV("rev", "Read git revision of a file"),
    SYMBOL("symbol", "Read content by Java/Kotlin canonicalName"),

    ;

    companion object {
        fun all(): List<BuiltinAgent> {
            return values().toList()
        }
    }
}

class BuiltinAgentProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val builtinAgents = BuiltinAgent.all()
        builtinAgents.forEach {
            val withTypeText = LookupElementBuilder.create(it.agentName)
                .withIcon(DevInIcons.DEFAULT)
                .withTypeText(it.description, true)
                .withInsertHandler { context, _ ->
//                    context.document.insertString(context.tailOffset, ":")
//                    context.editor.caretModel.moveCaretRelatively(0, 1, false, false, false)
                }

            result.addElement(withTypeText)
        }
    }
}

