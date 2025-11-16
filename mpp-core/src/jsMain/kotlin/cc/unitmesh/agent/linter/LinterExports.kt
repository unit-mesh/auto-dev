package cc.unitmesh.agent.linter

import cc.unitmesh.agent.tool.shell.JsShellExecutor
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * JavaScript-friendly wrapper for LinterRegistry
 */
@JsExport
object JsLinterRegistry {
    private val registry = LinterRegistry.getInstance()

    /**
     * Get linter summary for specific files
     */
    @JsName("getLinterSummaryForFiles")
    fun getLinterSummaryForFiles(filePaths: Array<String>, projectPath: String = "."): Promise<JsLinterSummary> {
        return GlobalScope.promise {
            val summary = registry.getLinterSummaryForFiles(filePaths.toList(), projectPath)
            summary.toJs()
        }
    }

    /**
     * Find suitable linters for files
     */
    @JsName("findLintersForFiles")
    fun findLintersForFiles(filePaths: Array<String>): Array<String> {
        val linters = registry.findLintersForFiles(filePaths.toList())
        return linters.map { it.name }.toTypedArray()
    }

    /**
     * Get all registered linters
     */
    @JsName("getAllLinters")
    fun getAllLinters(): Array<String> {
        return registry.getAllLinters().map { it.name }.toTypedArray()
    }
}

/**
 * JavaScript-friendly wrapper for LintDetector
 */
@JsExport
class JsLintDetector(projectPath: String) {
    private val shellExecutor = JsShellExecutor()
    private val detector = LintDetector(shellExecutor)

    /**
     * Detect all available linters in the system
     */
    fun detectAvailableLinters(): Promise<Array<JsLinterDetectionResult>> {
        return GlobalScope.promise {
            val results = detector.detectAvailableLinters()
            results.map { it.toJs() }.toTypedArray()
        }
    }

    /**
     * Detect linters for specific files
     */
    fun detectLintersForFiles(filePaths: Array<String>): Promise<Array<JsLinterDetectionResult>> {
        return GlobalScope.promise {
            val results = detector.detectLintersForFiles(filePaths.toList())
            results.map { it.toJs() }.toTypedArray()
        }
    }

    /**
     * Get linter configuration by name
     */
    fun getLinterConfig(name: String): JsLinterConfig? {
        return detector.getLinterConfig(name)?.toJs()
    }

    /**
     * Get all linter configurations
     */
    fun getAllLinterConfigs(): Array<JsLinterConfig> {
        return detector.getAllLinterConfigs().map { it.toJs() }.toTypedArray()
    }
}

/**
 * JavaScript-friendly linter detection result
 */
@JsExport
data class JsLinterDetectionResult(
    val linterName: String,
    val isAvailable: Boolean,
    val version: String? = null,
    val supportedFiles: Array<String> = emptyArray(),
    val installationInstructions: String? = null
)

/**
 * JavaScript-friendly linter configuration
 */
@JsExport
data class JsLinterConfig(
    val name: String,
    val technology: String,
    val category: String,
    val fileExtensions: Array<String>,
    val versionCommand: String,
    val description: String
)

/**
 * JavaScript-friendly file lint summary
 */
@JsExport
data class JsFileLintSummary(
    val filePath: String,
    val linterName: String,
    val totalIssues: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val topIssues: Array<JsLintIssue>,
    val hasMoreIssues: Boolean
)

/**
 * JavaScript-friendly linter summary
 */
@JsExport
data class JsLinterSummary(
    val totalFiles: Int,
    val filesWithIssues: Int,
    val totalIssues: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val fileIssues: Array<JsFileLintSummary>,
    val executedLinters: Array<String>
)

/**
 * JavaScript-friendly lint issue
 */
@JsExport
data class JsLintIssue(
    val line: Int,
    val column: Int,
    val severity: String,
    val message: String,
    val rule: String? = null,
    val suggestion: String? = null,
    val filePath: String? = null
)

/**
 * JavaScript-friendly lint result
 */
@JsExport
data class JsLintResult(
    val filePath: String,
    val issues: Array<JsLintIssue>,
    val success: Boolean,
    val errorMessage: String? = null,
    val linterName: String
) {
    val hasIssues: Boolean get() = issues.isNotEmpty()
    val errorCount: Int get() = issues.count { it.severity == "ERROR" }
    val warningCount: Int get() = issues.count { it.severity == "WARNING" }
}

/**
 * JavaScript-friendly AI linter factory
 */
@JsExport
class JsAILinterFactory(
    projectPath: String,
    private val llmService: KoogLLMService
) {
    private val shellExecutor = JsShellExecutor()
    private val lintDetector = LintDetector(shellExecutor)
    private val factory = AILinterFactory(shellExecutor, llmService, lintDetector)
    
    /**
     * Create an AI linter for a specific linter name
     */
    fun createLinter(linterName: String): JsAILinter? {
        val linter = factory.createLinter(linterName)
        return linter?.let { JsAILinter(it) }
    }
    
    /**
     * Create AI linters for all detected available linters
     */
    fun createAvailableLinters(): Promise<Array<JsAILinter>> {
        return kotlinx.coroutines.GlobalScope.promise {
            val linters = factory.createAvailableLinters()
            linters.map { JsAILinter(it) }.toTypedArray()
        }
    }
    
    /**
     * Create AI linters for specific files
     */
    fun createLintersForFiles(filePaths: Array<String>): Promise<Array<JsAILinter>> {
        return kotlinx.coroutines.GlobalScope.promise {
            val linters = factory.createLintersForFiles(filePaths.toList())
            linters.map { JsAILinter(it) }.toTypedArray()
        }
    }
}

/**
 * JavaScript-friendly AI linter wrapper
 */
@JsExport
class JsAILinter(private val linter: AILinter) {
    val name: String get() = linter.name
    val description: String get() = linter.description
    val supportedExtensions: Array<String> get() = linter.supportedExtensions.toTypedArray()
    
    /**
     * Check if linter is available
     */
    fun isAvailable(): Promise<Boolean> {
        return kotlinx.coroutines.GlobalScope.promise {
            linter.isAvailable()
        }
    }
    
    /**
     * Lint a file
     */
    fun lintFile(filePath: String, projectPath: String): Promise<JsLintResult> {
        return kotlinx.coroutines.GlobalScope.promise {
            val result = linter.lintFile(filePath, projectPath)
            result.toJs()
        }
    }
    
    /**
     * Lint multiple files
     */
    fun lintFiles(filePaths: Array<String>, projectPath: String): Promise<Array<JsLintResult>> {
        return kotlinx.coroutines.GlobalScope.promise {
            val results = linter.lintFiles(filePaths.toList(), projectPath)
            results.map { it.toJs() }.toTypedArray()
        }
    }
    
    /**
     * Get installation instructions
     */
    fun getInstallationInstructions(): String {
        return linter.getInstallationInstructions()
    }
}

// Extension functions for conversion

private fun LinterDetectionResult.toJs(): JsLinterDetectionResult {
    return JsLinterDetectionResult(
        linterName = linterName,
        isAvailable = isAvailable,
        version = version,
        supportedFiles = supportedFiles.toTypedArray(),
        installationInstructions = installationInstructions
    )
}

private fun LinterConfig.toJs(): JsLinterConfig {
    return JsLinterConfig(
        name = name,
        technology = technology,
        category = category,
        fileExtensions = fileExtensions.toTypedArray(),
        versionCommand = versionCommand,
        description = description
    )
}

private fun FileLintSummary.toJs(): JsFileLintSummary {
    return JsFileLintSummary(
        filePath = filePath,
        linterName = linterName,
        totalIssues = totalIssues,
        errorCount = errorCount,
        warningCount = warningCount,
        infoCount = infoCount,
        topIssues = topIssues.map { it.toJs() }.toTypedArray(),
        hasMoreIssues = hasMoreIssues
    )
}

fun LinterSummary.toJs(): JsLinterSummary {
    return JsLinterSummary(
        totalFiles = totalFiles,
        filesWithIssues = filesWithIssues,
        totalIssues = totalIssues,
        errorCount = errorCount,
        warningCount = warningCount,
        infoCount = infoCount,
        fileIssues = fileIssues.map { it.toJs() }.toTypedArray(),
        executedLinters = executedLinters.toTypedArray()
    )
}

private fun LintIssue.toJs(): JsLintIssue {
    return JsLintIssue(
        line = line,
        column = column,
        severity = severity.name,
        message = message,
        rule = rule,
        suggestion = suggestion,
        filePath = filePath
    )
}

private fun LintResult.toJs(): JsLintResult {
    return JsLintResult(
        filePath = filePath,
        issues = issues.map { it.toJs() }.toTypedArray(),
        success = success,
        errorMessage = errorMessage,
        linterName = linterName
    )
}

