package cc.unitmesh.agent

import cc.unitmesh.agent.codereview.ModifiedCodeRange
import cc.unitmesh.agent.linter.LintFileResult
import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.devins.compiler.template.TemplateCompiler
import cc.unitmesh.devins.compiler.variable.VariableType

/**
 * Renders system prompts for the code review agent using templates and context
 *
 * Simplified to only two prompt templates:
 * 1. Analysis Prompt - for analyzing code and lint results
 * 2. Fix Generation Prompt - for generating actionable fixes
 */
class CodeReviewAgentPromptRenderer {
    val logger = getLogger("CodeReviewAgentPromptRenderer")

    fun renderAnalysisPrompt(
        reviewType: String,
        filePaths: List<String>,
        codeContent: Map<String, String>,
        lintResults: Map<String, String>,
        diffContext: String = "",
        toolList: String = "",
        language: String = "EN"
    ): String {
        val template = when (language.uppercase()) {
            "ZH", "CN" -> CodeReviewAnalysisTemplate.ZH
            else -> CodeReviewAnalysisTemplate.EN
        }

        val formattedFiles = codeContent.entries.joinToString("\n\n") { (path, content) ->
            """### File: $path
```
$content
```"""
        }

        val formattedLintResults = if (lintResults.isEmpty()) {
            "No linter issues found."
        } else {
            lintResults.entries.joinToString("\n\n") { (path, result) ->
                """### Lint Results for: $path
```
$result
```"""
            }
        }

        val variableTable = cc.unitmesh.devins.compiler.variable.VariableTable()
        variableTable.addVariable("reviewType", VariableType.STRING, reviewType)
        variableTable.addVariable("fileCount", VariableType.STRING, filePaths.size.toString())
        variableTable.addVariable("filePaths", VariableType.STRING, filePaths.joinToString("\n- ", prefix = "- "))
        variableTable.addVariable("codeContent", VariableType.STRING, formattedFiles)
        variableTable.addVariable("lintResults", VariableType.STRING, formattedLintResults)
        variableTable.addVariable(
            "diffContext",
            VariableType.STRING,
            if (diffContext.isNotBlank()) "\n\n### Diff Context\n$diffContext" else ""
        )
        variableTable.addVariable("toolList", VariableType.STRING, toolList)

        val compiler = TemplateCompiler(variableTable)
        val prompt = compiler.compile(template)

        logger.debug { "Generated analysis prompt (${prompt.length} chars)" }
        return prompt
    }

    /**
     * Renders fix generation prompt for creating actionable fixes
     * This is the second step in the code review process
     *
     * @param changedHunks Map of file paths to changed code hunks (extracted from diff)
     * @param lintResults Lint results for the changed files
     * @param analysisOutput The analysis output from Phase 1
     * @param language Language for the prompt (EN or ZH)
     */
    fun renderFixGenerationPrompt(
        changedHunks: Map<String, List<cc.unitmesh.agent.vcs.context.CodeHunk>>,
        lintResults: List<LintFileResult>,
        analysisOutput: String,
        userFeedback: String = "",
        language: String = "EN"
    ): String {
        val template = when (language.uppercase()) {
            "ZH", "CN" -> FixGenerationTemplate.ZH
            else -> FixGenerationTemplate.EN
        }

        // Format changed code blocks (not full files!)
        val formattedChangedCode = if (changedHunks.isNotEmpty()) {
            changedHunks.entries.joinToString("\n\n") { (filePath, hunks) ->
                buildString {
                    appendLine("### File: $filePath")
                    appendLine()

                    hunks.forEachIndexed { index, hunk ->
                        appendLine("#### Changed Block #${index + 1}")
                        appendLine("**Location**: Lines ${hunk.newStartLine}-${hunk.newStartLine + hunk.newLineCount - 1}")
                        appendLine("**Changes**: +${hunk.addedLines.size} lines, -${hunk.deletedLines.size} lines")
                        appendLine()
                        appendLine("```diff")
                        appendLine(hunk.header)

                        // Show context before
                        hunk.contextBefore.forEach { line ->
                            appendLine(" $line")
                        }

                        // Show deleted lines
                        hunk.deletedLines.forEach { line ->
                            appendLine("-$line")
                        }

                        // Show added lines
                        hunk.addedLines.forEach { line ->
                            appendLine("+$line")
                        }

                        // Show context after
                        hunk.contextAfter.forEach { line ->
                            appendLine(" $line")
                        }

                        appendLine("```")
                        appendLine()
                    }
                }
            }
        } else {
            "No changed code blocks available."
        }

        // Format lint results - only for files in changedHunks, with priority separation
        val relevantFiles = changedHunks.keys
        val relevantLintResults = lintResults.filter { it.filePath in relevantFiles }

        // Separate files by priority: Error files vs Warning-only files
        val filesWithErrors = relevantLintResults.filter { it.errorCount > 0 }.sortedByDescending { it.errorCount }
        val filesWithWarningsOnly = relevantLintResults.filter { it.errorCount == 0 && it.warningCount > 0 }

        val formattedLintResults = if (relevantLintResults.isNotEmpty()) {
            buildString {
                // Section 1: Files with ERRORS (🚨 CRITICAL PRIORITY)
                if (filesWithErrors.isNotEmpty()) {
                    appendLine("## 🚨 CRITICAL PRIORITY - Files with Errors (MUST FIX FIRST)")
                    appendLine()
                    appendLine("**${filesWithErrors.size} file(s) with compilation/lint errors:**")
                    appendLine()

                    filesWithErrors.forEach { fileResult ->
                        val totalCount = fileResult.errorCount + fileResult.warningCount + fileResult.infoCount
                        appendLine("### ❌ ${fileResult.filePath}")
                        appendLine("**Priority: CRITICAL** - ${fileResult.errorCount} error(s), ${fileResult.warningCount} warning(s)")
                        appendLine()

                        val critical = fileResult.issues.filter { it.severity == LintSeverity.ERROR }
                        if (critical.isNotEmpty()) {
                            appendLine("**🔴 ERRORS (Fix Required):**")
                            critical.forEach { issue ->
                                appendLine("- Line ${issue.line}: ${issue.message}")
                                val ruleText = issue.rule
                                if (ruleText != null && ruleText.isNotBlank()) {
                                    appendLine("  Rule: `$ruleText`")
                                }
                            }
                            appendLine()
                        }

                        val warnings = fileResult.issues.filter { it.severity == LintSeverity.WARNING }
                        if (warnings.isNotEmpty()) {
                            appendLine("**⚠️ Warnings (Fix if related to errors):**")
                            warnings.take(3).forEach { issue ->
                                appendLine("- Line ${issue.line}: ${issue.message}")
                            }
                            if (warnings.size > 3) {
                                appendLine("... and ${warnings.size - 3} more warnings")
                            }
                            appendLine()
                        }
                    }
                    appendLine("---")
                    appendLine()
                }

                // Section 2: Files with WARNINGS only (Lower priority)
                if (filesWithWarningsOnly.isNotEmpty()) {
                    appendLine("## ⚠️ LOWER PRIORITY - Files with Warnings Only")
                    appendLine()
                    appendLine("**${filesWithWarningsOnly.size} file(s) with warnings (optional fixes):**")
                    appendLine()

                    filesWithWarningsOnly.forEach { fileResult ->
                        appendLine("### ${fileResult.filePath}")
                        appendLine("${fileResult.warningCount} warning(s) - Fix these only after addressing all errors")
                        appendLine()

                        val warnings = fileResult.issues.filter { it.severity == LintSeverity.WARNING }
                        warnings.take(3).forEach { issue ->
                            appendLine("- Line ${issue.line}: ${issue.message}")
                        }
                        if (warnings.size > 3) {
                            appendLine("... and ${warnings.size - 3} more warnings")
                        }
                        appendLine()
                    }
                }
            }
        } else {
            "No lint issues found."
        }

        val variableTable = cc.unitmesh.devins.compiler.variable.VariableTable()
        variableTable.addVariable(
            "changedCode",
            VariableType.STRING,
            formattedChangedCode
        )
        variableTable.addVariable(
            "lintResults",
            VariableType.STRING,
            formattedLintResults
        )
        variableTable.addVariable(
            "analysisOutput",
            VariableType.STRING,
            analysisOutput
        )
        variableTable.addVariable(
            "userFeedback",
            VariableType.STRING,
            if (userFeedback.isNotBlank()) "\n\n### User Feedback/Instructions\n$userFeedback" else ""
        )

        val compiler = TemplateCompiler(variableTable)
        val prompt = compiler.compile(template)

        logger.debug { "Generated fix generation prompt (${prompt.length} chars) for ${changedHunks.size} files" }
        return prompt
    }

    /**
     * Renders modification plan prompt for generating concise, structured suggestions
     * This is called after analysis and before fix generation
     *
     * @param lintResults Lint results for files
     * @param analysisOutput Analysis output from walkthrough
     * @param modifiedCodeRanges Map of file path to list of modified code ranges (functions, classes)
     * @param language Language for the prompt (EN or ZH)
     */
    fun renderModificationPlanPrompt(
        lintResults: List<LintFileResult>,
        analysisOutput: String,
        modifiedCodeRanges: Map<String, List<ModifiedCodeRange>> = emptyMap(),
        language: String = "EN"
    ): String {
        val template = when (language.uppercase()) {
            "ZH", "CN" -> ModificationPlanTemplate.ZH
            else -> ModificationPlanTemplate.EN
        }

        // Format lint results summary with specific errors for modification plan
        // Group issues by function/class context for cleaner display
        val lintSummary = formatSummary(lintResults, modifiedCodeRanges)

        val variableTable = cc.unitmesh.devins.compiler.variable.VariableTable()
        variableTable.addVariable("lintResults", VariableType.STRING, lintSummary)
        variableTable.addVariable("analysisOutput", VariableType.STRING, analysisOutput)

        val compiler = TemplateCompiler(variableTable)
        val prompt = compiler.compile(template)

        logger.debug { "Generated modification plan prompt (${prompt.length} chars)" }
        return prompt
    }

    fun formatSummary(
        lintResults: List<LintFileResult>,
        modifiedCodeRanges: Map<String, List<ModifiedCodeRange>>
    ): String {
        if (lintResults.isEmpty()) {
            return "No lint issues found."
        }

        val totalErrors = lintResults.sumOf { it.errorCount }
        val totalWarnings = lintResults.sumOf { it.warningCount }
        val filesWithErrors = lintResults.filter { it.errorCount > 0 }.sortedByDescending { it.errorCount }
        val filesWithWarningsOnly = lintResults.filter { it.errorCount == 0 && it.warningCount > 0 }

        return buildString {
            appendLine("**Summary**: $totalErrors error(s), $totalWarnings warning(s) across ${lintResults.size} file(s)")
            appendLine()

            // Files with errors (CRITICAL)
            if (filesWithErrors.isNotEmpty()) {
                appendLine("## 🔴 CRITICAL - Files with Errors (MUST FIX)")
                appendLine()
                filesWithErrors.forEach { file ->
                    formatFileIssues(file, modifiedCodeRanges, this)
                }
                appendLine("---")
                appendLine()
            }

            // Files with warnings only (Lower priority)
            if (filesWithWarningsOnly.isNotEmpty()) {
                appendLine("## ⚠️ WARNINGS ONLY - Lower Priority")
                appendLine()
                appendLine("**${filesWithWarningsOnly.size} file(s) with warnings only:**")
                appendLine()
                filesWithWarningsOnly.forEach { file ->
                    formatFileIssues(file, modifiedCodeRanges, this, showWarningsOnly = true)
                }
            }
        }
    }

    private fun formatFileIssues(
        file: LintFileResult,
        modifiedCodeRanges: Map<String, List<ModifiedCodeRange>>,
        output: StringBuilder,
        showWarningsOnly: Boolean = false
    ) {
        output.appendLine("### File: ${file.filePath}")
        output.appendLine("**Total**: ${file.errorCount} error(s), ${file.warningCount} warning(s)")
        output.appendLine()

        val errors = file.issues.filter { it.severity == LintSeverity.ERROR }
        val warnings = file.issues.filter { it.severity == LintSeverity.WARNING }

        // Format errors
        if (errors.isNotEmpty() && !showWarningsOnly) {
            formatIssuesByContext(file.filePath, errors, modifiedCodeRanges, "Errors", output)
        }

        // Format warnings
        if (warnings.isNotEmpty()) {
            val label = if (showWarningsOnly) "Warnings" else "Warnings (${warnings.size} total, grouped by function/class)"
            formatIssuesByContext(file.filePath, warnings, modifiedCodeRanges, label, output, maxContexts = if (showWarningsOnly) 5 else 3, showDetails = !showWarningsOnly)
        }

        output.appendLine()
    }

    private fun formatIssuesByContext(
        filePath: String,
        issues: List<LintIssue>,
        modifiedCodeRanges: Map<String, List<ModifiedCodeRange>>,
        label: String,
        output: StringBuilder,
        maxContexts: Int = Int.MAX_VALUE,
        showDetails: Boolean = true
    ) {
        val issuesByContext = issues.groupBy { issue ->
            findFunctionContext(filePath, issue.line, modifiedCodeRanges)
        }

        output.appendLine("**$label:**")
        
        issuesByContext.entries.take(maxContexts).forEach { (context, contextIssues) ->
            val header = if (context != null) {
                if (showDetails) {
                    "**In `${context.elementName}` (${context.elementType.lowercase()}, lines ${context.startLine}-${context.endLine})**:"
                } else {
                    "In `${context.elementName}` (${context.elementType.lowercase()})"
                }
            } else {
                if (showDetails) {
                    "**No specific function context**:"
                } else {
                    "No specific function context"
                }
            }
            output.appendLine("- $header")

            if (showDetails) {
                contextIssues.forEach { issue ->
                    output.appendLine("  - Line ${issue.line}: ${issue.message}")
                    if (issue.rule != null && issue.rule.isNotBlank()) {
                        output.appendLine("    - Rule: `${issue.rule}`")
                    }
                    if (issue.suggestion != null && issue.suggestion.isNotBlank()) {
                        output.appendLine("    - Suggestion: ${issue.suggestion}")
                    }
                }
            } else {
                // For warnings in error files, just show line numbers
                val lines = contextIssues.map { it.line }.sorted().joinToString(", ")
                output.appendLine("  - ${contextIssues.size} warning(s) at lines: $lines")
            }
            output.appendLine()
        }

        if (issuesByContext.size > maxContexts) {
            output.appendLine("... and ${issuesByContext.size - maxContexts} more contexts")
            output.appendLine()
        }
    }

    /**
     * Find the function/class context for a given line number
     * Returns the most specific (smallest) context that contains the line
     */
    private fun findFunctionContext(
        filePath: String,
        lineNumber: Int,
        modifiedCodeRanges: Map<String, List<ModifiedCodeRange>>
    ): ModifiedCodeRange? {
        val ranges = modifiedCodeRanges[filePath] ?: return null
        return ranges
            .filter { lineNumber in it.startLine..it.endLine }
            .minByOrNull { it.endLine - it.startLine }
    }
}


