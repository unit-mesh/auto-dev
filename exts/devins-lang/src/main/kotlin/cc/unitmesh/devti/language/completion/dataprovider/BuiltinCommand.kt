package cc.unitmesh.devti.language.completion.dataprovider

import cc.unitmesh.devti.language.DevInIcons
import com.intellij.icons.AllIcons
import java.nio.charset.StandardCharsets
import javax.swing.Icon

enum class BuiltinCommand(
    val commandName: String,
    val description: String,
    val icon: Icon,
    val hasCompletion: Boolean = false,
    val requireProps: Boolean = false,
) {
    FILE("file", "Read the content of a file", AllIcons.Actions.Copy, true, true),
    REV("rev", "Read git change by file", AllIcons.Vcs.History, true, true),

    /**
     * Every language will have a symbol completion, which is the most basic completion, for example,
     * - Java: [com.intellij.codeInsight.completion.JavaKeywordCompletion]
     * - Kotlin: [org.jetbrains.kotlin.idea.completion.KotlinCompletionContributor]
     * - Python: [com.jetbrains.python.codeInsight.completion.PyClassNameCompletionContributor]
     */
    SYMBOL(
        "symbol",
        "Read content by Java/Kotlin canonicalName",
        AllIcons.Toolwindows.ToolWindowStructure,
        true,
        true
    ),
    WRITE("write", "Write content to a file, /write:path/to/file:L1-L2", AllIcons.Actions.Edit, true, true),
    PATCH("patch", "Apply patch to a file, /patch:path/to/file", AllIcons.Vcs.Patch_file, false),
    RUN("run", "Run the content of a file", AllIcons.Actions.Execute, true, true),
    SHELL("shell", "Run shell command", DevInIcons.Terminal, true, true),
    COMMIT("commit", "Commit the content of a file", AllIcons.Vcs.CommitNode, false),
    FILE_FUNC(
        "file-func",
        "Read the name of a file, support for: " + FileFunc.values().joinToString(",") { it.funcName },
        AllIcons.Actions.GroupByFile,
        true,
        true
    ),
    ;

    companion object {
        fun all(): List<BuiltinCommand> {
            return values().toList()
        }

        fun example(command: BuiltinCommand): String {
            val commandName = command.commandName
            val inputStream = BuiltinCommand::class.java.getResourceAsStream("/agent/toolExamples/$commandName.devin")
            if (inputStream == null) {
                throw IllegalStateException("Example file not found: $commandName.devin")
            }

            return inputStream!!.use {
                it.readAllBytes().toString(StandardCharsets.UTF_8)
            }
        }

        fun fromString(agentName: String): BuiltinCommand? {
            return values().find { it.commandName == agentName }
        }
    }
}