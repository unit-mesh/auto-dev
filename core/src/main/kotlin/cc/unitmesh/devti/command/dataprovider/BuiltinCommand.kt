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
    FILE(
        "file", 
        "Read and retrieve file content from project using relative path. Essential for examining existing code, configurations, or documentation before modifications. Supports line ranges (L1-L10) and global filename search. Returns complete file content with line numbers for context understanding.",
        AllIcons.FileTypes.Any_type,
        true, 
        true
    ),
    REV(
        "rev",
        "Retrieve Git revision details and change history by commit SHA hash. Use when you need to understand code evolution, review specific commits, or analyze change patterns. For complex Git operations, prefer native Git commands. Returns commit metadata, changed files, and diff information.",
        AllIcons.Vcs.History,
        true,
        true,
        enableInSketch = false
    ),
    SYMBOL(
        "symbol",
        "Resolve and locate symbol definitions using Java/Kotlin canonical names (package.class.method format). Essential for understanding code structure, finding class definitions, method signatures, and field declarations. Returns precise symbol location with complete context including types and documentation.",
        AllIcons.Toolwindows.ToolWindowStructure,
        true,
        true,
        enableInSketch = false,
    ),
    WRITE(
        "write",
        "Create new files or completely replace existing file content using markdown code blocks. Use for new file creation or major rewrites. Always verify file existence with /file first. Supports line range specification (L1-L2). Creates proper directory structure automatically.",
        AllIcons.Actions.Edit,
        true,
        true,
        enableInSketch = true
    ),
    PATCH(
        "patch",
        "Apply precise code modifications using GNU unified diff format. Preferred for targeted changes to existing files while preserving surrounding code. Use when making multiple changes or complex modifications, prefer /write command for better reliability.",
        AllIcons.Vcs.Patch_file,
        false,
        enableInSketch = false
    ),
    EDIT_FILE(
        "edit_file",
        "Apply structured file edits using target_file, instructions, and code_edit parameters. Designed for precise code modifications with clear context markers. Use // ... existing code ... to represent unchanged sections. Ideal for targeted edits with explicit instructions.",
        AllIcons.Actions.Edit,
        false
    ),
    RUN(
        "run",
        "Execute IntelliJ IDEA's built-in build and test commands. Use for running Gradle tasks, Maven goals, npm scripts, or test suites. Essential for validating code changes and ensuring project builds correctly. Returns execution output, exit codes, and error information.",
        AllIcons.Actions.Execute,
        true,
        true,
        enableInSketch = false
    ),

    LIBRARY_VERSION_FETCH(
        "library-version-fetch",
        "Fetch latest version of npmjs, maven or other libraries.",
        AllIcons.Nodes.PpLib,
        true,
        true,
        enableInSketch = false
    ),

    SHELL(
        "shell",
        "Execute shell commands in project environment using ProcessBuilder. Use for system operations, build scripts, environment setup, or external tool execution. Commands run in project directory context. Returns stdout, stderr, and exit codes. Handle with security considerations.",
        AllIcons.Debugger.Console,
        true,
        true,
        enableInSketch = false
    ),
    COMMIT(
        "commit", 
        "Execute Git commit operation for current workspace changes. Use after completing code modifications to save progress. Requires commit message following conventional commit format (feat:, fix:, docs:, etc.). Commits all staged changes in current workspace.",
        AllIcons.Vcs.CommitNode, 
        false
    ),
    BROWSE(
        "browse", 
        "Fetch and retrieve content from external URLs and web resources. Use when gathering information from documentation, APIs, or external references. Returns web page content as text for analysis. Essential for researching external dependencies or documentation.",
        AllIcons.Toolwindows.WebToolWindow, 
        false, 
        true
    ),
    REFACTOR(
        "refactor",
        "Perform safe code refactoring operations using IntelliJ IDEA's refactoring engine. Supports rename, safe delete, and move operations while maintaining all references. Use for restructuring code while preserving functionality. Ensures code integrity during complex refactoring tasks.",
        AutoDevIcons.IDEA,
        true,
        true
    ),
    STRUCTURE(
        "structure",
        "Analyze and extract code structure using AST/PSI parsing. Returns architectural overview including class names, method signatures, field declarations, and inheritance relationships. Essential for understanding codebase organization before making modifications.",
        AllIcons.Toolwindows.ToolWindowStructure,
        true,
        true,
        enableInSketch = false
    ),
    DIR(
        "dir", 
        "Explore project structure by listing files and directories in tree format. Use when you need to understand project organization or locate specific files. Default depth 2, intelligently handles deep structures. Excludes binary files and VCS-ignored content. Essential for project navigation.",
        AllIcons.Actions.ProjectDirectory, 
        true, 
        true
    ),
    DATABASE(
        "database",
        "Interact with project databases for schema inspection and SQL operations. Use when working with data-driven applications. Supports schema listing, table inspection, and SQL query execution (SELECT, INSERT, UPDATE). Returns database structure or query results with proper formatting.",
        AllIcons.Toolwindows.ToolWindowHierarchy,
        true,
        true
    ),
    LOCAL_SEARCH(
        "localSearch",
        "Search for text patterns within project scope with surrounding context. Use when finding code references, similar implementations, or understanding concept usage. Returns matches with 5 lines before/after for context. Minimum 4 characters required for search terms.",
        AllIcons.Actions.Search,
        false,
        true
    ),
    RELATED(
        "related",
        "Discover related code components using AST analysis and dependency relationships. Use when understanding code architecture, finding connected components, or analyzing impact of changes. Returns structurally related classes, methods, and modules for better code comprehension.",
        AllIcons.Actions.Find,
        false,
        true
    ),
    OPEN(
        "open", 
        "Open specified file in IDE editor for viewing or manual editing. Use when preparing files for inspection or making them available for subsequent operations. Input: file path relative to project root. Makes file active in editor interface.",
        AllIcons.Actions.MenuOpen, 
        false, 
        true
    ),
    RIPGREP_SEARCH(
        "ripgrepSearch", 
        "Perform high-speed regex-based text search across entire project using ripgrep tool. Use for complex pattern matching and large-scale code searches. Supports full regex syntax including advanced patterns. More powerful than localSearch for complex queries.",
        AllIcons.Actions.Regex, 
        false, 
        true
    ),
    RULE(
        "rule",
        "Retrieve project-specific coding rules and specifications from prompts/rule/ directory. Use when understanding project conventions before writing code. Returns coding standards, style guides, and quality requirements. Essential for generating compliant code that matches project standards.",
        AutoDevIcons.RULE,
        true,
        true
    ),
    USAGE(
        "usage",
        "Find all usages of classes, methods, or fields across the project codebase. Use when understanding impact of changes or how components are utilized. Input: fully qualified name (package.class.method). Returns caller locations with file paths and usage context. Critical for safe refactoring decisions.",
        AllIcons.Actions.DynamicUsages,
        true,
        true
    ),
    TOOLCHAIN_COMMAND(
        "x",
        "Access specialized toolchain functions and domain-specific integrations through plugin ecosystem. Use when standard commands are insufficient and you need specialized tools. Provides extensibility for custom development workflows and external tool integration.",
        AllIcons.Actions.Execute,
        true,
        false,
        enableInSketch = false
    ),
    LAUNCH_PROCESS(
        "launch-process",
        "Launch a new process with specified command and options. Supports background execution, timeout control, and environment variable configuration. Returns process ID for management and monitoring. Essential for running external tools and scripts.",
        AllIcons.Actions.Execute,
        true,
        true,
        enableInSketch = false
    ),
    LIST_PROCESSES(
        "list-processes",
        "List all active and terminated processes managed by the system. Shows process status, command, working directory, and execution times. Use for monitoring running processes and debugging execution issues.",
        AllIcons.General.TodoDefault,
        false,
        false,
        enableInSketch = false
    ),
    KILL_PROCESS(
        "kill-process",
        "Terminate a running process by its process ID. Supports both graceful termination and force kill options. Use when processes need to be stopped or are consuming excessive resources.",
        AllIcons.Actions.Suspend,
        true,
        true,
        enableInSketch = false
    ),
    READ_PROCESS_OUTPUT(
        "read-process-output",
        "Read stdout and stderr output from a running or completed process. Supports streaming output and output size limits. Essential for monitoring process execution and debugging.",
        AllIcons.Actions.Show,
        true,
        true,
        enableInSketch = false
    ),
    WRITE_PROCESS_INPUT(
        "write-process-input",
        "Write input data to a running process's stdin. Supports interactive process communication and automation of command-line tools. Use for processes that require user input or commands.",
        AllIcons.Actions.Edit,
        true,
        true,
        enableInSketch = false
    ),
    A2A(
        "a2a",
        "Send message to A2A (Agent-to-Agent) protocol agents. Use for delegating tasks to specialized AI agents that support A2A protocol. Specify agent name and message content. Returns agent response for further processing or analysis.",
        AutoDevIcons.A2A,
        true,
        true,
        enableInSketch = true
    ),
    AGENTS(
        "agents",
        "List all available AI agents or invoke a specific agent. Without parameters, displays all available agents including A2A agents and DevIns agents. With agent name parameter, invokes the specified agent for task execution.",
        AutoDevIcons.CLOUD_AGENT,
        true,
        false,
        enableInSketch = true
    ),
    SPECKIT(
        "speckit",
        "Execute GitHub Spec-Kit commands for Spec-Driven Development. Supports subcommands like /speckit.clarify, /speckit.specify, /speckit.plan, /speckit.tasks, /speckit.implement, etc. Loads prompts from .github/prompts/ directory and executes spec-driven workflows.",
        AutoDevIcons.IDEA,
        true,
        true,
        enableInSketch = true
    ),
    CLAUDE_SKILL(
        "skill",
        "Execute Claude Skills for specialized agent capabilities. Skills are organized folders of instructions, scripts, and resources that agents can discover and load dynamically. Supports subcommands like /skill.pdf, /skill.algorithmic-art, etc. Loads skills from project directories containing SKILL.md files or ~/.claude/skills/ directory.",
        AutoDevIcons.IDEA,
        true,
        true,
        enableInSketch = true
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