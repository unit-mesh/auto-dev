package cc.unitmesh.devti.command.dataprovider

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.agent.Tool
import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import java.nio.charset.StandardCharsets
import javax.swing.Icon

enum class BuiltinCommand(
    val commandName: String,
    override val description: String,
    val icon: Icon,
    val hasCompletion: Boolean = false,
    val requireProps: Boolean = false,
    val enableInSketch: Boolean = true
) : Tool {
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
        true,
        enableInSketch = false,
    ),
    WRITE(
        "write",
        "Write content to a file with markdown code block, /write:path/to/file:L1-L2",
        AllIcons.Actions.Edit,
        true,
        true,
        enableInSketch = false
    ),
    PATCH(
        "patch",
        "Apply GNU unified diff format structure patch to a file, /patch:path/to/file",
        AllIcons.Vcs.Patch_file,
        false
    ),
    RUN(
        "run",
        "Run the IDE's built-in command, like build tool, test.",
        AllIcons.Actions.Execute,
        true,
        true,
        enableInSketch = false
    ),
    SHELL(
        "shell",
        "Execute a shell command and collect (ProcessBuild) the result",
        AllIcons.Debugger.Console,
        true,
        true,
        enableInSketch = false
    ),
    COMMIT("commit", "Do commit with current workspace with some messages.", AllIcons.Vcs.CommitNode, false),
    BROWSE("browse", "Fetch the content of a given URL.", AllIcons.Toolwindows.WebToolWindow, false, true),
    REFACTOR(
        "refactor",
        "Refactor the content of a file, only support for rename, safeDelete and move.",
        AutoDevIcons.IDEA,
        true,
        true
    ),
    STRUCTURE(
        "structure",
        "Get the structure of a file with AST/PSI",
        AllIcons.Toolwindows.ToolWindowStructure,
        true,
        true,
        enableInSketch = false
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
    RULE(
        "rule",
        "Get the rule from one file, such as code style, code quality, and code smell",
        AutoDevIcons.RULE,
        true,
        true
    ),
    USAGE(
        "usage",
        "Get the usage of a class, method, or field in the project",
        AllIcons.Actions.DynamicUsages,
        true,
        true
    ),
    TOOLCHAIN_COMMAND(
        "x",
        "Execute custom toolchain command",
        AllIcons.Actions.Execute,
        true,
        false,
        enableInSketch = false
    ),
    ;

    companion object {
        fun all(): List<BuiltinCommand> {
            return entries.filter { it != TOOLCHAIN_COMMAND }
        }

        fun example(command: BuiltinCommand): String {
            val commandName = command.commandName
            return example(commandName)
        }

        fun example(commandName: String): String {
            val inputStream = BuiltinCommand::class.java.getResourceAsStream("/agent/toolExamples/$commandName.devin")
            if (inputStream == null) {
                return "<DevInsError> Example file not found: $commandName.devin"
            }

            return inputStream.use {
                it.readAllBytes().toString(StandardCharsets.UTF_8)
            }
        }

        suspend fun fromString(commandName: String): BuiltinCommand? {
            val builtinCommand = entries.find { it.commandName == commandName }
            if (builtinCommand != null) {
                return builtinCommand
            }

            val providerName = toolchainProviderName(commandName)
            val provider = ToolchainFunctionProvider.lookup(providerName)
            if (provider != null) {
                return TOOLCHAIN_COMMAND
            }

            ToolchainFunctionProvider.all().forEach {
                if (it.funcNames().contains(commandName)) {
                    return TOOLCHAIN_COMMAND
                }
            }

            val project = ProjectManager.getInstance().openProjects.first()
            AutoDevNotifications.warn(project, "Command not found: $commandName")
            return null
        }

        fun toolchainProviderName(commandName: String): String {
            val commandProviderName = commandName.substring(0, 1).uppercase() + commandName.substring(1)
            val providerName = commandProviderName + "FunctionProvider"
            return providerName
        }

        suspend fun allToolchains(project: Project?): List<AgentTool> {
            if (project == null) return emptyList()

            return ToolchainFunctionProvider.all().map {
                val toolInfo: List<AgentTool> = it.toolInfos(project)
                if (toolInfo.isNotEmpty()) {
                    return@map toolInfo
                }

                it.funcNames().map { funcName ->
                    AgentTool(funcName, "", "")
                }
            }.flatten()
        }
    }
}