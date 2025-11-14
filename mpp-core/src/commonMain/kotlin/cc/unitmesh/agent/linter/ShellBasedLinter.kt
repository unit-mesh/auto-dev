package cc.unitmesh.agent.linter

import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutionConfig
import cc.unitmesh.agent.logging.getLogger

/**
 * Base class for linters that run as shell commands
 */
abstract class ShellBasedLinter(
    private val shellExecutor: ShellExecutor
) : Linter {
    
    private val logger = getLogger("ShellBasedLinter")
    
    /**
     * Get the command to check if linter is installed
     */
    abstract fun getVersionCommand(): String
    
    /**
     * Get the command to lint a file
     */
    abstract fun getLintCommand(filePath: String, projectPath: String): String
    
    /**
     * Parse the output of the linter into LintIssues
     */
    abstract fun parseOutput(output: String, filePath: String): List<LintIssue>
    
    override suspend fun isAvailable(): Boolean {
        return try {
            val config = ShellExecutionConfig(timeoutMs = 5000L)
            val result = shellExecutor.execute(getVersionCommand(), config)
            result.exitCode == 0
        } catch (e: Exception) {
            logger.warn { "Linter $name not available: ${e.message}" }
            false
        }
    }

    override suspend fun lintFile(filePath: String, projectPath: String): LintResult {
        try {
            if (!isAvailable()) {
                return LintResult(
                    filePath = filePath,
                    issues = emptyList(),
                    success = false,
                    errorMessage = "Linter $name is not installed. ${getInstallationInstructions()}",
                    linterName = name
                )
            }

            val command = getLintCommand(filePath, projectPath)
            logger.info { "Running linter: $command" }

            val config = ShellExecutionConfig(
                workingDirectory = projectPath,
                timeoutMs = 30000L
            )
            val result = shellExecutor.execute(command, config)

            // Many linters return non-zero exit code when issues are found
            // So we consider it successful if we can parse the output
            val issues = parseOutput(result.stdout, filePath)

            return LintResult(
                filePath = filePath,
                issues = issues,
                success = true,
                errorMessage = if (result.exitCode != 0 && issues.isEmpty()) result.stderr else null,
                linterName = name
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to lint file $filePath with $name" }
            return LintResult(
                filePath = filePath,
                issues = emptyList(),
                success = false,
                errorMessage = "Failed to run linter: ${e.message}",
                linterName = name
            )
        }
    }
}

/**
 * ESLint/Biome linter for JavaScript/TypeScript
 */
class BiomeLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "biome"
    override val description = "Fast formatter and linter for JavaScript, TypeScript, JSON, and CSS"
    override val supportedExtensions = listOf("js", "jsx", "ts", "tsx", "json", "css")
    
    override fun getVersionCommand() = "biome --version"
    
    override fun getLintCommand(filePath: String, projectPath: String) = 
        "biome check --reporter=json \"$filePath\""
    
    override fun parseOutput(output: String, filePath: String): List<LintIssue> {
        // Parse Biome JSON output
        // This is a simplified version - real implementation would use JSON parsing
        val issues = mutableListOf<LintIssue>()
        
        try {
            // Biome outputs JSON format, we'll parse it simply here
            // In real implementation, use kotlinx.serialization
            val lines = output.lines()
            for (line in lines) {
                if (line.contains("\"severity\"")) {
                    // Extract basic info from JSON line
                    val severity = when {
                        line.contains("error") -> LintSeverity.ERROR
                        line.contains("warning") -> LintSeverity.WARNING
                        else -> LintSeverity.INFO
                    }
                    
                    issues.add(LintIssue(
                        line = 0, // Would extract from JSON
                        column = 0,
                        severity = severity,
                        message = "Biome issue found", // Would extract from JSON
                        rule = null,
                        filePath = filePath
                    ))
                }
            }
        } catch (e: Exception) {
            // Fallback to simple parsing
        }
        
        return issues
    }
    
    override fun getInstallationInstructions() = 
        "Install Biome: npm install -g @biomejs/biome or pnpm add -g @biomejs/biome"
}

/**
 * Detekt linter for Kotlin
 */
class DetektLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "detekt"
    override val description = "Static code analysis for Kotlin"
    override val supportedExtensions = listOf("kt", "kts")
    
    override fun getVersionCommand() = "detekt --version"
    
    override fun getLintCommand(filePath: String, projectPath: String) = 
        "detekt --input \"$filePath\" --report txt:stdout"
    
    override fun parseOutput(output: String, filePath: String): List<LintIssue> {
        val issues = mutableListOf<LintIssue>()
        
        // Parse detekt output format
        // Example: File.kt:10:5: warning: Line is too long
        val pattern = Regex("""(.+):(\d+):(\d+):\s*(error|warning|info):\s*(.+)""")
        
        for (line in output.lines()) {
            val match = pattern.find(line)
            if (match != null) {
                val (_, lineNum, col, severityStr, message) = match.destructured
                
                val severity = when (severityStr.lowercase()) {
                    "error" -> LintSeverity.ERROR
                    "warning" -> LintSeverity.WARNING
                    else -> LintSeverity.INFO
                }
                
                issues.add(LintIssue(
                    line = lineNum.toIntOrNull() ?: 0,
                    column = col.toIntOrNull() ?: 0,
                    severity = severity,
                    message = message,
                    rule = null,
                    filePath = filePath
                ))
            }
        }
        
        return issues
    }
    
    override fun getInstallationInstructions() = 
        "Install Detekt: Add to build.gradle.kts or download CLI from https://github.com/detekt/detekt"
}

/**
 * Ruff linter for Python
 */
class RuffLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "ruff"
    override val description = "Fast Python linter"
    override val supportedExtensions = listOf("py")
    
    override fun getVersionCommand() = "ruff --version"
    
    override fun getLintCommand(filePath: String, projectPath: String) = 
        "ruff check \"$filePath\" --output-format=json"
    
    override fun parseOutput(output: String, filePath: String): List<LintIssue> {
        val issues = mutableListOf<LintIssue>()
        
        // Parse ruff JSON output
        // Simplified parsing - real implementation would use JSON parser
        try {
            val lines = output.lines()
            for (line in lines) {
                if (line.contains("\"code\"")) {
                    issues.add(LintIssue(
                        line = 0,
                        column = 0,
                        severity = LintSeverity.WARNING,
                        message = "Ruff issue found",
                        rule = null,
                        filePath = filePath
                    ))
                }
            }
        } catch (e: Exception) {
            // Fallback
        }
        
        return issues
    }
    
    override fun getInstallationInstructions() = 
        "Install Ruff: pip install ruff or brew install ruff"
}

/**
 * ShellCheck linter for shell scripts
 */
class ShellCheckLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "shellcheck"
    override val description = "Static analysis tool for shell scripts"
    override val supportedExtensions = listOf("sh", "bash")

    override fun getVersionCommand() = "shellcheck --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "shellcheck -f json \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> {
        val issues = mutableListOf<LintIssue>()

        // Parse shellcheck JSON output
        try {
            val lines = output.lines()
            for (line in lines) {
                if (line.contains("\"level\"")) {
                    val severity = when {
                        line.contains("error") -> LintSeverity.ERROR
                        line.contains("warning") -> LintSeverity.WARNING
                        else -> LintSeverity.INFO
                    }

                    issues.add(LintIssue(
                        line = 0,
                        column = 0,
                        severity = severity,
                        message = "ShellCheck issue found",
                        rule = null,
                        filePath = filePath
                    ))
                }
            }
        } catch (e: Exception) {
            // Fallback
        }

        return issues
    }

    override fun getInstallationInstructions() =
        "Install ShellCheck: brew install shellcheck or apt-get install shellcheck"
}

/**
 * PMD linter for Java
 */
class PMDLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "pmd"
    override val description = "Source code analyzer for Java and other languages"
    override val supportedExtensions = listOf("java")

    override fun getVersionCommand() = "pmd --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "pmd check -d \"$filePath\" -f text -R rulesets/java/quickstart.xml"

    override fun parseOutput(output: String, filePath: String): List<LintIssue> {
        val issues = mutableListOf<LintIssue>()

        // Parse PMD output format
        // Example: /path/to/File.java:10:	Rule violation message
        val pattern = Regex("""(.+):(\d+):\s*(.+)""")

        for (line in output.lines()) {
            val match = pattern.find(line)
            if (match != null) {
                val (_, lineNum, message) = match.destructured

                // PMD doesn't always specify severity in text format, default to WARNING
                val severity = when {
                    message.contains("error", ignoreCase = true) -> LintSeverity.ERROR
                    else -> LintSeverity.WARNING
                }

                issues.add(LintIssue(
                    line = lineNum.toIntOrNull() ?: 0,
                    column = 0,
                    severity = severity,
                    message = message.trim(),
                    rule = null,
                    filePath = filePath
                ))
            }
        }

        return issues
    }

    override fun getInstallationInstructions() =
        "Install PMD: brew install pmd or download from https://pmd.github.io/"
}

