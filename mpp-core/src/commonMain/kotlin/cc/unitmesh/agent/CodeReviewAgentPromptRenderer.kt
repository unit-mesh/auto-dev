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
    ): String = if (lintResults.isNotEmpty()) {
        buildString {
            val totalErrors = lintResults.sumOf { it.errorCount }
            val totalWarnings = lintResults.sumOf { it.warningCount }

            appendLine("**Summary**: $totalErrors error(s), $totalWarnings warning(s) across ${lintResults.size} file(s)")
            appendLine()

            // Show files with errors, grouped by function context
            val filesWithErrors = lintResults.filter { it.errorCount > 0 }.sortedByDescending { it.errorCount }
            if (filesWithErrors.isNotEmpty()) {
                appendLine("## 🔴 CRITICAL - Files with Errors (MUST FIX)")
                appendLine()

                filesWithErrors.forEach { file ->
                    appendLine("### File: ${file.filePath}")
                    appendLine("**Total**: ${file.errorCount} error(s), ${file.warningCount} warning(s)")
                    appendLine()

                    // Group errors by function context
                    val errors = file.issues.filter { it.severity == LintSeverity.ERROR }
                    if (errors.isNotEmpty()) {
                        val errorsByContext = groupIssuesByContext(file.filePath, errors, modifiedCodeRanges)

                        appendLine("**Errors (grouped by function/class):**")
                        errorsByContext.forEach { (contextKey, issues) ->
                            val context = contextKey.context
                            val header = if (context != null) {
                                "**In `${context.elementName}` (${context.elementType.lowercase()}, lines ${context.startLine}-${context.endLine})**:"
                            } else {
                                "**No specific function context**:"
                            }
                            appendLine("- $header")

                            issues.forEach { issue ->
                                appendLine("  - Line ${issue.line}: ${issue.message}")
                                if (issue.rule != null && issue.rule.isNotBlank()) {
                                    appendLine("    - Rule: `${issue.rule}`")
                                }
                                if (issue.suggestion != null && issue.suggestion.isNotBlank()) {
                                    appendLine("    - Suggestion: ${issue.suggestion}")
                                }
                            }
                            appendLine()
                        }
                    }

                    // Show warnings for the same file (if any), also grouped
                    val warnings = file.issues.filter { issue -> issue.severity == LintSeverity.WARNING }
                    if (warnings.isNotEmpty()) {
                        val warningsByContext = groupIssuesByContext(file.filePath, warnings, modifiedCodeRanges)

                        appendLine("**Warnings** (${warnings.size} total, grouped by function/class):")
                        warningsByContext.entries.take(3).forEach { entry ->
                            val context = entry.key.context
                            val issues = entry.value
                            val header = if (context != null) {
                                "In `${context.elementName}` (${context.elementType.lowercase()})"
                            } else {
                                "No specific function context"
                            }
                            val lines = issues.map { issue -> issue.line }.sorted().joinToString(", ")
                            appendLine("- $header - ${issues.size} warning(s) at lines: $lines")
                        }
                        if (warningsByContext.size > 3) {
                            appendLine("... and ${warningsByContext.size - 3} more contexts with warnings")
                        }
                        appendLine()
                    }
                }
                appendLine("---")
                appendLine()
            }

            // Show warning-only files (grouped by function context)
            val filesWithWarningsOnly = lintResults.filter { it.errorCount == 0 && it.warningCount > 0 }
            if (filesWithWarningsOnly.isNotEmpty()) {
                appendLine("## ⚠️ WARNINGS ONLY - Lower Priority")
                appendLine()
                appendLine("**${filesWithWarningsOnly.size} file(s) with warnings only:**")
                appendLine()

                val totalWarningsCount = filesWithWarningsOnly.sumOf { it.warningCount }
                appendLine("Total warnings: $totalWarningsCount across ${filesWithWarningsOnly.size} file(s)")
                appendLine()

                // Show all files with warnings grouped by function context
                filesWithWarningsOnly.forEach { file ->
                    appendLine("### File: ${file.filePath}")
                    appendLine("**Total**: ${file.warningCount} warning(s)")
                    appendLine()

                    // Group warnings by context (function/class)
                    val warnings = file.issues.filter { issue -> issue.severity == LintSeverity.WARNING }
                    val warningsByContext = groupIssuesByContext(file.filePath, warnings, modifiedCodeRanges)

                    warningsByContext.entries.take(5).forEach { entry ->
                        val context = entry.key.context
                        val issues = entry.value
                        if (context != null) {
                            val lines = issues.map { issue -> issue.line }.sorted().joinToString(", ")
                            appendLine("- **In `${context.elementName}` (${context.elementType.lowercase()})**: ${issues.size} warning(s) at lines $lines")
                            // Show first warning message as example
                            val firstIssue = issues.first()
                            appendLine("  - Example: ${firstIssue.message}")
                            if (firstIssue.rule != null && firstIssue.rule!!.isNotBlank()) {
                                appendLine("  - Rule: `${firstIssue.rule}`")
                            }
                        } else {
                            // No context - group by rule instead
                            val byRule = issues.groupBy { issue -> issue.rule ?: "no-rule" }
                            byRule.forEach { (rule, ruleIssues) ->
                                val lines = ruleIssues.map { issue -> issue.line }.sorted().joinToString(", ")
                                appendLine("- **Rule `$rule`**: ${ruleIssues.size} warning(s) at lines $lines")
                                appendLine("  - ${ruleIssues.first().message}")
                            }
                        }
                    }

                    if (warningsByContext.size > 5) {
                        appendLine("... and ${warningsByContext.size - 5} more function/class contexts with warnings")
                    }
                    appendLine()
                }
            }
        }
    } else {
        "No lint issues found."
    }

    private data class ContextKey(val context: ModifiedCodeRange?)

    /**
     * Group lint issues by their function/class context
     * Issues in the same function will be grouped together
     *
     * @param filePath The file path
     * @param issues List of lint issues to group
     * @param modifiedCodeRanges Map of file path to modified code ranges
     * @return Map of context to list of issues in that context
     */
    private fun groupIssuesByContext(
        filePath: String,
        issues: List<LintIssue>,
        modifiedCodeRanges: Map<String, List<ModifiedCodeRange>>
    ): Map<ContextKey, List<LintIssue>> {
        return issues.groupBy { issue ->
            val context = findFunctionContext(filePath, issue.line, modifiedCodeRanges)
            ContextKey(context)
        }
    }

    /**
     * Find the function/class context for a given line number
     * Returns the most specific (smallest) context that contains the line
     *
     * @param filePath The file path
     * @param lineNumber The line number to find context for
     * @param modifiedCodeRanges Map of file path to modified code ranges
     * @return The code range containing this line, or null if not found
     */
    private fun findFunctionContext(
        filePath: String,
        lineNumber: Int,
        modifiedCodeRanges: Map<String, List<ModifiedCodeRange>>
    ): ModifiedCodeRange? {
        val ranges = modifiedCodeRanges[filePath] ?: return null
        val matchingContexts = ranges.filter { range ->
            lineNumber in range.startLine..range.endLine
        }

        return matchingContexts.minByOrNull { it.endLine - it.startLine }
    }
}


