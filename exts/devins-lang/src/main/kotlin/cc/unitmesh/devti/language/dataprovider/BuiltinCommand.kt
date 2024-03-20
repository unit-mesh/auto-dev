package cc.unitmesh.devti.language.dataprovider

import com.intellij.icons.AllIcons
import javax.swing.Icon

enum class BuiltinCommand(
    val commandName: String,
    val description: String,
    val icon: Icon,
    val hasCompletion: Boolean = false,
    val requireProps: Boolean = false,
) {
    FILE("file", "Read the content of a file", AllIcons.Actions.AddFile, true, true),
    REV("rev", "Read git change by file", AllIcons.Vcs.History, true, true),

    /**
     * Every language will have a symbol completion, which is the most basic completion, for example,
     * - Java: [com.intellij.codeInsight.completion.JavaKeywordCompletion]
     * - Kotlin: [org.jetbrains.kotlin.idea.completion.KotlinCompletionContributor]
     * - Python: [com.jetbrains.python.codeInsight.completion.PyClassNameCompletionContributor]
     */
    SYMBOL("symbol", "[TODO] Read content by Java/Kotlin canonicalName", AllIcons.Actions.GroupBy, true, true),
    WRITE("write", "Write content to a file, /write:path/to/file:L1-L2", AllIcons.Actions.Edit, true, true),
    PATCH("patch", "Apply patch to a file, /patch:path/to/file", AllIcons.Vcs.Patch_file, false),
    RUN("run", "Run the content of a file", AllIcons.Actions.Execute, true, true),
    FILE_FUNC("file-func", "Read the name of a file", AllIcons.Actions.GroupByFile, true, true),
    COMMIT("commit", "Commit the content of a file", AllIcons.Vcs.CommitNode, false)
    ;

    companion object {
        fun all(): List<BuiltinCommand> {
            return values().toList()
        }

        fun fromString(agentName: String): BuiltinCommand? {
            return values().find { it.commandName == agentName }
        }
    }
}