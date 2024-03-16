package cc.unitmesh.devti.language.completion

import cc.unitmesh.devti.language.DevInIcons
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.util.ProcessingContext
import javax.swing.Icon

enum class BuiltinCommand(val agentName: String, val description: String, val icon: Icon) {
    FILE("file", "Read the content of a file", AllIcons.Actions.AddFile),
    REV("rev", "Read git change by file", AllIcons.Vcs.History),
    SYMBOL("symbol", "Read content by Java/Kotlin canonicalName", AllIcons.Actions.GroupBy),
    WRITE("write", "Write content to a file, /write:/path/to/file:L1-L2", AllIcons.Actions.Edit),
    ;

    companion object {
        fun all(): List<BuiltinCommand> {
            return values().toList()
        }

        fun fromString(agentName: String): BuiltinCommand? {
            return values().find { it.agentName == agentName }
        }
    }
}

class BuiltinCommandProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        BuiltinCommand.all().forEach {
            result.addElement(
                LookupElementBuilder.create(it.agentName)
                    .withIcon(it.icon)
                    .withTypeText(it.description, true)
            )
        }
    }
}

