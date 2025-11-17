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
     */
    fun renderFixGenerationPrompt(
        codeContent: Map<String, String>,
        lintResults: List<LintFileResult>,
        analysisOutput: String,
        language: String = "EN"
    ): String {
        val template = when (language.uppercase()) {
            "ZH", "CN" -> FixGenerationTemplate.ZH
            else -> FixGenerationTemplate.EN
        }

        // Format code content
        val formattedCode = if (codeContent.isNotEmpty()) {
            codeContent.entries.joinToString("\n\n") { (path, content) ->
                """### File: $path
```
$content
```"""
            }
        } else {
            "No code content available."
        }

        // Format lint results
        val formattedLintResults = if (lintResults.isNotEmpty()) {
            lintResults.mapNotNull { fileResult ->
                if (fileResult.issues.isNotEmpty()) {
                    val totalCount = fileResult.errorCount + fileResult.warningCount + fileResult.infoCount
                    buildString {
                        appendLine("### ${fileResult.filePath}")
                        appendLine("Total Issues: $totalCount (${fileResult.errorCount} errors, ${fileResult.warningCount} warnings)")
                        appendLine()

                        val critical = fileResult.issues.filter { it.severity == LintSeverity.ERROR }
                        val warnings = fileResult.issues.filter { it.severity == LintSeverity.WARNING }

                        if (critical.isNotEmpty()) {
                            appendLine("**Critical Issues:**")
                            critical.forEach { issue ->
                                appendLine("- Line ${issue.line}: ${issue.message}")
                                val ruleText = issue.rule
                                if (ruleText != null && ruleText.isNotBlank()) {
                                    appendLine("  Rule: $ruleText")
                                }
                            }
                            appendLine()
                        }

                        if (warnings.isNotEmpty()) {
                            appendLine("**Warnings:**")
                            warnings.take(5).forEach { issue ->
                                appendLine("- Line ${issue.line}: ${issue.message}")
                                val ruleText = issue.rule
                                if (ruleText != null && ruleText.isNotBlank()) {
                                    appendLine("  Rule: $ruleText")
                                }
                            }
                            if (warnings.size > 5) {
                                appendLine("... and ${warnings.size - 5} more warnings")
                            }
                        }
                    }
                } else {
                    null
                }
            }.joinToString("\n\n")
        } else {
            "No lint issues found."
        }

        val variableTable = cc.unitmesh.devins.compiler.variable.VariableTable()
        variableTable.addVariable(
            "codeContent",
            VariableType.STRING,
            formattedCode
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

        val compiler = TemplateCompiler(variableTable)
        val prompt = compiler.compile(template)

        logger.debug { "Generated fix generation prompt (${prompt.length} chars)" }
        return prompt
    }
}


