package cc.unitmesh.devti.devin.dataprovider

import cc.unitmesh.devti.AutoDevIcons
import com.intellij.icons.AllIcons
import java.nio.charset.StandardCharsets
import javax.swing.Icon

enum class BuiltinCommand(
    val commandName: String,
    val description: String,
    val icon: Icon,
    val hasCompletion: Boolean = false,
    val requireProps: Boolean = false,
    val enableInSketch: Boolean = true
) {
    FILE("file", "Read the content of a file by project relative path", AllIcons.Actions.Copy, true, true),
    REV(
        "rev",
        "Read git changes by sha hash; For other git operations, it is recommended to use native git commands",
        AllIcons.Vcs.History,
        true,
        true,
        enableInSketch = false
    ),

    /**
     * Every language will have a symbol completion, which is the most basic completion, for example,
     * - Java: [com.intellij.codeInsight.completion.JavaKeywordCompletion]
     * - Kotlin: [org.jetbrains.kotlin.idea.completion.KotlinCompletionContributor]
     * - Python: [com.jetbrains.python.codeInsight.completion.PyClassNameCompletionContributor]
     */
    SYMBOL(
        "symbol",
        "Read content by Java/Kotlin canonical name, such as package name, class name.",
        AllIcons.Toolwindows.ToolWindowStructure,
        true,
        true
    ),
    WRITE(
        "write",
        "Write content to a file with markdown code block, /write:path/to/file:L1-L2",
        AllIcons.Actions.Edit,
        true,
        true
    ),
    PATCH(
        "patch",
        "Apply GNU unified diff format structure patch to a file, /patch:path/to/file",
        AllIcons.Vcs.Patch_file,
        false
    ),
    RUN("run", "Run the IDE's built-in command, like build tool, test.", AllIcons.Actions.Execute, true, true),
    SHELL(
        "shell",
        "Execute a shell command and collect (ProcessBuild) the result",
        AllIcons.Debugger.Console,
        true,
        true
    ),
    COMMIT("commit", "Do commit with current workspace with some messages.", AllIcons.Vcs.CommitNode, false),
    FILE_FUNC(
        "file-func",
        "Read the name of a file, support for: " + FileFunc.values().joinToString(",") { it.funcName },
        AllIcons.Actions.GroupByFile,
        true,
        true,
        enableInSketch = false,
    ),
    BROWSE("browse", "Fetch the content of a given URL.", AllIcons.Toolwindows.WebToolWindow, false, true),
    REFACTOR(
        "refactor",
        "Refactor the content of a file, only support for rename, safeDelete and move.",
        AutoDevIcons.Idea,
        true,
        true
    ),
    STRUCTURE(
        "structure",
        "Get the structure of a file with AST/PSI",
        AllIcons.Toolwindows.ToolWindowStructure,
        true,
        true
    ),
    DIR("dir", "List files and directories in a tree-like structure", AllIcons.Actions.ProjectDirectory, true, true),
    DATABASE(
        "database",
        "Read the content of a database, /database:query\n```sql\nSELECT * FROM table\n```",
        AllIcons.Toolwindows.ToolWindowHierarchy,
        true,
        true
    ),
    LOCAL_SEARCH(
        "localSearch",
        "Search text in the scope (current only support project) will return 5 line before and after",
        AllIcons.Actions.Search,
        false,
        true
    ),
    RELATED(
        "related",
        "Get related code by AST (abstract syntax tree) for the current file",
        AllIcons.Actions.Find,
        false,
        true
    ),
    OPEN("open", "Open a file in the editor", AllIcons.Actions.MenuOpen, false, true),
    RIPGREP_SEARCH("ripgrepSearch", "Search text in the project with ripgrep", AllIcons.Actions.Regex, false, true),
    ;

    companion object {
        fun all(): List<BuiltinCommand> {
            return values().toList()
        }

        fun example(command: BuiltinCommand): String {
            val commandName = command.commandName
            val inputStream = BuiltinCommand::class.java.getResourceAsStream("/agent/toolExamples/$commandName.devin")
                ?: throw IllegalStateException("Example file not found: $commandName.devin")

            return inputStream.use {
                it.readAllBytes().toString(StandardCharsets.UTF_8)
            }
        }

        fun fromString(agentName: String): BuiltinCommand? = values().find { it.commandName == agentName }

        val READ_COMMANDS = setOf(DIR, LOCAL_SEARCH, FILE, REV, STRUCTURE, SYMBOL, DATABASE, RELATED, RIPGREP_SEARCH)
    }
}