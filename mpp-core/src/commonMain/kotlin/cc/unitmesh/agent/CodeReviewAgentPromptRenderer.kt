package cc.unitmesh.agent

import cc.unitmesh.agent.linter.LintFileResult
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

}


