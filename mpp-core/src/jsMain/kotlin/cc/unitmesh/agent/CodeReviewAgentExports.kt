package cc.unitmesh.agent

import cc.unitmesh.agent.config.JsToolConfigFile
import cc.unitmesh.agent.language.LanguageDetector
import cc.unitmesh.agent.linter.LintFileResult
import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * JS-friendly version of ReviewTask
 */
@JsExport
data class JsReviewTask(
    val filePaths: Array<String>,
    val reviewType: String = "COMPREHENSIVE",
    val projectPath: String,
    val additionalContext: String = ""
) {
    fun toCommon(): ReviewTask {
        val type = try {
            ReviewType.valueOf(reviewType.uppercase())
        } catch (e: Exception) {
            ReviewType.COMPREHENSIVE
        }

        return ReviewTask(
            filePaths = filePaths.toList(),
            reviewType = type,
            projectPath = projectPath,
            additionalContext = additionalContext
        )
    }
}

/**
 * JS-friendly version of ReviewFinding
 */
@JsExport
data class JsReviewFinding(
    val severity: String,
    val category: String,
    val description: String,
    val filePath: String? = null,
    val lineNumber: Int? = null,
    val suggestion: String? = null
) {
    companion object {
        fun fromCommon(finding: ReviewFinding): JsReviewFinding {
            return JsReviewFinding(
                severity = finding.severity.name,
                category = finding.category,
                description = finding.description,
                filePath = finding.filePath,
                lineNumber = finding.lineNumber,
                suggestion = finding.suggestion
            )
        }
    }
}

/**
 * JS-friendly version of CodeReviewResult
 */
@JsExport
data class JsCodeReviewResult(
    val success: Boolean,
    val message: String,
    val findings: Array<JsReviewFinding>
) {
    companion object {
        fun fromCommon(result: CodeReviewResult): JsCodeReviewResult {
            return JsCodeReviewResult(
                success = result.success,
                message = result.message,
                findings = result.findings.map { JsReviewFinding.fromCommon(it) }.toTypedArray()
            )
        }
    }
}

/**
 * JS-friendly version of LintResult
 */
@JsExport
data class JsLintResult(
    val filePath: String,
    val issues: Array<JsLintIssue>,
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * JS-friendly version of LintIssue
 */
@JsExport
data class JsLintIssue(
    val line: Int,
    val column: Int,
    val severity: String,
    val message: String,
    val rule: String? = null,
    val suggestion: String? = null
)

/**
 * JS-friendly version of LintFileResult
 */
@JsExport
data class JsLintFileResult(
    val filePath: String,
    val linterName: String,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val issues: Array<JsLintIssue>
)

/**
 * JS-friendly Code Review Agent for CLI usage
 */
@JsExport
class JsCodeReviewAgent(
    private val projectPath: String,
    private val llmService: cc.unitmesh.llm.JsKoogLLMService,
    private val maxIterations: Int = 50,
    private val renderer: JsCodingAgentRenderer? = null,
    private val mcpServers: dynamic = null,
    private val toolConfig: JsToolConfigFile? = null
) {
    // Internal Kotlin CodeReviewAgent
    private val agent: CodeReviewAgent = CodeReviewAgent(
        projectPath = projectPath,
        llmService = llmService.service,
        maxIterations = maxIterations,
        renderer = if (renderer != null) JsRendererAdapter(renderer) else DefaultCodingAgentRenderer(),
        mcpToolConfigService = createToolConfigService(toolConfig)
    )

    /**
     * Create tool config service from JS tool config
     */
    private fun createToolConfigService(jsToolConfig: JsToolConfigFile?): cc.unitmesh.agent.config.McpToolConfigService {
        return if (jsToolConfig != null) {
            cc.unitmesh.agent.config.McpToolConfigService(jsToolConfig.toCommon())
        } else {
            cc.unitmesh.agent.config.McpToolConfigService(cc.unitmesh.agent.config.ToolConfigFile())
        }
    }

    /**
     * Execute code review task (main analysis method)
     * This is the unified method that replaces both analyzeIntent and analyzeWithDataDriven
     *
     * @param reviewType Type of review (e.g., "COMPREHENSIVE", "SECURITY")
     * @param filePaths Array of file paths to review
     * @param codeContent Object mapping file paths to their content
     * @param lintResults Object mapping file paths to their lint results (formatted strings)
     * @param diffContext Optional diff context string
     * @param commitMessage Optional commit message for intent analysis
     * @param commitId Optional commit ID
     * @param repoUrl Optional repository URL for issue tracking
     * @param issueToken Optional issue tracker token
     * @param useTools Whether to use tools for analysis (default: false for data-driven mode)
     * @param analyzeIntent Whether to perform intent analysis (default: false)
     * @param language Language for the prompt ("EN" or "ZH")
     * @param onChunk Optional callback for streaming response chunks
     * @return Promise resolving to JsAnalysisResult
     */
    @JsName("executeTask")
    fun executeTask(
        reviewType: String,
        filePaths: Array<String> = emptyArray(),
        codeContent: dynamic = null,
        lintResults: dynamic = null,
        diffContext: String = "",
        commitMessage: String = "",
        commitId: String = "",
        repoUrl: String = "",
        issueToken: String = "",
        useTools: Boolean = false,
        analyzeIntent: Boolean = false,
        language: String = "EN",
        onChunk: ((String) -> Unit)? = null
    ): Promise<JsAnalysisResult> {
        return GlobalScope.promise {
            // Convert JS dynamic objects to Kotlin maps
            val codeContentMap = if (codeContent != null) {
                convertDynamicToMap(codeContent)
            } else {
                emptyMap()
            }

            val lintResultsMap = if (lintResults != null) {
                convertDynamicToMap(lintResults)
            } else {
                emptyMap()
            }

            // Create ReviewTask
            val reviewTypeEnum = try {
                ReviewType.valueOf(reviewType.uppercase())
            } catch (e: Exception) {
                ReviewType.COMPREHENSIVE
            }

            val task = ReviewTask(
                filePaths = filePaths.toList(),
                reviewType = reviewTypeEnum,
                projectPath = projectPath,
                additionalContext = diffContext
            )

            // Execute the task
            val result = agent.execute(task, onProgress = onChunk ?: {})

            // Return unified result
            JsAnalysisResult(
                success = result.success,
                content = result.content,
                mermaidDiagram = null,  // Not supported in basic execution
                issuesAnalyzed = emptyArray(),
                usedTools = false
            )
        }
    }



    /**
     * Generate fixes for identified issues
     * Uses code content, lint results, and analysis output to provide actionable fixes
     *
     * @param codeContent Object mapping file paths to their content
     * @param lintResults Array of JS lint results
     * @param analysisOutput The AI analysis output from previous analysis step
     * @param language Language for the prompt ("EN" or "ZH")
     * @param onChunk Optional callback for streaming response chunks
     * @return Promise resolving to the fix generation result as markdown string
     */
    @JsName("generateFixes")
    fun generateFixes(
        codeContent: dynamic,
        lintResults: Array<JsLintFileResult>,
        analysisOutput: String,
        language: String = "EN",
        onChunk: ((String) -> Unit)? = null
    ): Promise<String> {
        return GlobalScope.promise {
            // Convert JS dynamic object to Kotlin map
            val codeContentMap = convertDynamicToMap(codeContent)

            // Convert JS lint results to Kotlin LintFileResult
            val kotlinLintResults = lintResults.map { jsResult ->
                LintFileResult(
                    filePath = jsResult.filePath,
                    linterName = jsResult.linterName,
                    errorCount = jsResult.errorCount,
                    warningCount = jsResult.warningCount,
                    infoCount = jsResult.infoCount,
                    issues = jsResult.issues.map { jsIssue ->
                        LintIssue(
                            line = jsIssue.line,
                            column = jsIssue.column,
                            severity = when (jsIssue.severity.uppercase()) {
                                "ERROR" -> LintSeverity.ERROR
                                "WARNING" -> LintSeverity.WARNING
                                else -> LintSeverity.INFO
                            },
                            message = jsIssue.message,
                            rule = jsIssue.rule,
                            suggestion = jsIssue.suggestion
                        )
                    }
                )
            }

            val result = agent.generateFixes(
                codeContent = codeContentMap,
                lintResults = kotlinLintResults,
                analysisOutput = analysisOutput,
                language = language,
                onProgress = onChunk ?: {}
            )

            result.content
        }
    }



    /**
     * Helper to convert JS dynamic object to Kotlin Map<String, String>
     */
    private fun convertDynamicToMap(obj: dynamic): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (obj != null && obj != undefined) {
            val keys = js("Object.keys(obj)") as Array<String>
            for (key in keys) {
                val value = obj[key]
                if (value != null && value != undefined) {
                    map[key] = value.toString()
                }
            }
        }
        return map
    }
}

/**
 * Export-friendly analysis result
 */
@JsExport
data class JsAnalysisResult(
    val success: Boolean,
    val content: String,
    val mermaidDiagram: String?,
    val issuesAnalyzed: Array<String>,
    val usedTools: Boolean
)

/**
 * Diff-related exports for analyzing changes
 */
@JsExport
data class JsDiffFile(
    val path: String,
    val oldPath: String?,
    val changeType: String,
    val addedLines: Int,
    val deletedLines: Int,
    val hunks: Array<JsDiffHunk>
)

@JsExport
data class JsDiffHunk(
    val oldStartLine: Int,
    val oldLineCount: Int,
    val newStartLine: Int,
    val newLineCount: Int,
    val header: String,
    val addedLines: Array<String>,
    val deletedLines: Array<String>,
    val contextLines: Array<String>
)

/**
 * Parse git diff and extract changed files
 */
@JsExport
object JsDiffUtils {
    /**
     * Parse git diff output and return list of changed files
     * Simplified version that doesn't require mpp-ui dependency
     */
    @JsName("parseDiff")
    fun parseDiff(diffContent: String): Array<JsDiffFile> {
        val files = mutableListOf<JsDiffFile>()
        val lines = diffContent.lines()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            if (line.startsWith("diff --git ")) {
                val regex = Regex("""diff --git a/(.*?) b/(.*?)$""")
                val match = regex.find(line)
                if (match != null) {
                    val oldPath = match.groupValues[1]
                    val newPath = match.groupValues[2]
                    var changeType = "EDIT"

                    // Look ahead for file status
                    var j = i + 1
                    while (j < lines.size && j < i + 10) {
                        when {
                            lines[j].startsWith("new file mode") -> changeType = "CREATE"
                            lines[j].startsWith("deleted file mode") -> changeType = "DELETE"
                            lines[j].startsWith("@@") -> break
                        }
                        j++
                    }

                    if (newPath != "/dev/null") {
                        files.add(JsDiffFile(
                            path = newPath,
                            oldPath = if (oldPath != "/dev/null") oldPath else null,
                            changeType = changeType,
                            addedLines = 0,
                            deletedLines = 0,
                            hunks = emptyArray()
                        ))
                    }
                }
            }
            i++
        }

        return files.toTypedArray()
    }

    /**
     * Extract file paths from diff
     */
    @JsName("extractFilePaths")
    fun extractFilePaths(diffContent: String): Array<String> {
        val paths = mutableSetOf<String>()
        val lines = diffContent.lines()

        for (line in lines) {
            when {
                // Git diff format: diff --git a/path b/path
                line.startsWith("diff --git ") -> {
                    val regex = Regex("""diff --git a/(.*?) b/(.*?)$""")
                    val match = regex.find(line)
                    if (match != null) {
                        val path = match.groupValues[2]
                        if (path != "/dev/null") {
                            paths.add(path)
                        }
                    }
                }
                // Standard diff format: +++ b/path
                line.startsWith("+++ ") -> {
                    var path = line.substring(4).trim()
                    // Remove b/ prefix if present
                    if (path.startsWith("b/")) {
                        path = path.substring(2)
                    }
                    if (path != "/dev/null" && path.isNotEmpty()) {
                        paths.add(path)
                    }
                }
            }
        }

        return paths.toTypedArray()
    }
}

/**
 * Linter exports for JS
 */
@JsExport
class JsLinterRegistry {
    private val registry = cc.unitmesh.agent.linter.LinterRegistry.getInstance()

    /**
     * Find suitable linters for files
     */
    @JsName("findLintersForFiles")
    fun findLintersForFiles(filePaths: Array<String>): Array<String> {
        val linters = registry.findLintersForFiles(filePaths.toList())
        return linters.map { it.name }.toTypedArray()
    }

    /**
     * Detect language from file path
     */
    @JsName("detectLanguage")
    fun detectLanguage(filePath: String): String? {
        return LanguageDetector.detectLanguage(filePath)
    }

    /**
     * Get recommended linters for a language
     */
    @JsName("getLintersForLanguage")
    fun getLintersForLanguage(language: String): Array<String> {
        return LanguageDetector.getLinterNamesForLanguage(language).toTypedArray()
    }
}

