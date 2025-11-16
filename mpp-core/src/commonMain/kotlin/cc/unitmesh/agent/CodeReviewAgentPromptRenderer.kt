package cc.unitmesh.agent

import cc.unitmesh.agent.linter.LintFileResult
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.devins.compiler.template.TemplateCompiler

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
        variableTable.addVariable("reviewType", cc.unitmesh.devins.compiler.variable.VariableType.STRING, reviewType)
        variableTable.addVariable(
            "fileCount",
            cc.unitmesh.devins.compiler.variable.VariableType.STRING,
            filePaths.size.toString()
        )
        variableTable.addVariable(
            "filePaths",
            cc.unitmesh.devins.compiler.variable.VariableType.STRING,
            filePaths.joinToString("\n- ", prefix = "- ")
        )
        variableTable.addVariable(
            "codeContent",
            cc.unitmesh.devins.compiler.variable.VariableType.STRING,
            formattedFiles
        )
        variableTable.addVariable(
            "lintResults",
            cc.unitmesh.devins.compiler.variable.VariableType.STRING,
            formattedLintResults
        )
        variableTable.addVariable(
            "diffContext",
            cc.unitmesh.devins.compiler.variable.VariableType.STRING,
            if (diffContext.isNotBlank()) "\n\n### Diff Context\n$diffContext" else ""
        )

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
            cc.unitmesh.devins.compiler.variable.VariableType.STRING,
            formattedCode
        )
        variableTable.addVariable(
            "lintResults",
            cc.unitmesh.devins.compiler.variable.VariableType.STRING,
            formattedLintResults
        )
        variableTable.addVariable(
            "analysisOutput",
            cc.unitmesh.devins.compiler.variable.VariableType.STRING,
            analysisOutput
        )

        val compiler = TemplateCompiler(variableTable)
        val prompt = compiler.compile(template)

        logger.debug { "Generated fix generation prompt (${prompt.length} chars)" }
        return prompt
    }
}


object CodeReviewAnalysisTemplate {
    val EN = """
# Code Review Analysis

You are an expert code reviewer. Analyze the provided code and linter results to identify the **TOP 10 HIGHEST PRIORITY** issues.

## Task

Review Type: **${'$'}{reviewType}**
Files to Review: **${'$'}{fileCount}** files

${'$'}{filePaths}

## Code Content

${'$'}{codeContent}

## Linter Results

${'$'}{lintResults}
${'$'}{diffContext}

## Your Task

Provide a **CONCISE SUMMARY** of the top 3-5 critical/high priority issues of all significant issues.

**OUTPUT STRUCTURE**:
1. **Console Summary** (Brief - for terminal display)

---

##  Console Summary (Keep this SHORT)

### 📊 Quick Summary
One sentence overview of code quality.

### ⚠️ Top Priority Issues (Max 5)
For CRITICAL/HIGH issues only, list in this compact format:

#### #{number}. {Title}
**Severity**: CRITICAL | HIGH  
**Location**: `{file}:{line}` in `{method/class}`  
**Problem**: {One sentence description}  
**Fix**: {One sentence suggestion}

## Output Requirements

- Use proper Markdown formatting
- Start with Summary, then list exactly 5 issues (or fewer if less than 5 significant issues exist)
- Use clear section headers with emoji indicators (📊, 🚨)
- Keep total output concise and focused
""".trimIndent()

    val ZH = """
# 代码审查分析

你是一位专业的代码审查专家。分析提供的代码和 linter 结果，识别 **优先级最高的前 10 个问题**。

## 任务

审查类型：**${'$'}{reviewType}**
待审查文件：**${'$'}{fileCount}** 个文件

${'$'}{filePaths}

## 代码内容

${'$'}{codeContent}

## Linter 结果

${'$'}{lintResults}
${'$'}{diffContext}

## 你的任务

### ⚠️ 最高优先级问题（最多 5 个）
仅列出 CRITICAL/HIGH 问题，使用此简洁格式：

#### #{编号}. {标题}
**严重性**: CRITICAL | HIGH  
**位置**: `{文件}:{行号}` 在 `{方法/类}`  
**问题**: {一句话描述}  
**修复**: {一句话建议}

1. **按严重性排序**（使用严格标准）：
   - **CRITICAL**：仅用于必然导致安全漏洞、数据丢失或系统崩溃的问题
     - 示例：SQL 注入、泄露的密钥、关键路径中的空指针解引用
   - **HIGH**：必然导致错误行为或显著性能下降的问题
     - 示例：产生错误结果的逻辑错误、资源泄漏、竞态条件
   - **MEDIUM**：在特定条件下可能导致问题
     - 示例：缺少错误处理、次优算法、缺少验证
   - **LOW/INFO**：不影响功能的代码质量问题
     - 示例：代码重复、轻微样式不一致、缺少注释
3. **严重性评估规则**：
   - 除非有明确的 critical/high 影响证据，否则默认为 MEDIUM
   - Linter 警告应为 LOW/INFO，除非它们指示实际的 bug
   - 样式问题、命名约定、格式化 → 始终为 LOW/INFO
   - 缺少空检查 → MEDIUM（除非证明会导致崩溃 → HIGH）
   - 性能问题 → MEDIUM（除非通过测量证明是瓶颈 → HIGH）
4. **具体说明**：始终引用确切的 文件:行号 位置

""".trimIndent()
}

/**
 * Template for fix generation prompt
 * Generates unified diff patches for identified issues
 */
object FixGenerationTemplate {
    val EN = """
# Code Fix Generation - Unified Diff Format

Generate **unified diff patches** for the critical issues identified in the analysis.

## Original Code

${'$'}{codeContent}

## Lint Issues

${'$'}{lintResults}

## AI Analysis

${'$'}{analysisOutput}

## Your Task

Generate **unified diff patches** for the most critical issues. Use standard unified diff format.

### Required Format:

For each fix, provide a brief explanation followed by the diff patch:

#### Fix #{number}: {Brief Title}
**Issue**: {One-line description}
**Location**: {file}:{line}

```diff
diff --git a/{filepath} b/{filepath}
index {old_hash}..{new_hash} {mode}
--- a/{filepath}
+++ b/{filepath}
@@ -{old_start},{old_count} +{new_start},{new_count} @@ {context}
 {context line}
-{removed line}
+{added line}
 {context line}
```

### Example:

#### Fix #1: Fix null pointer exception
**Issue**: Missing null check for user parameter
**Location**: src/User.kt:15

```diff
diff --git a/src/User.kt b/src/User.kt
index abc1234..def5678 100644
--- a/src/User.kt
+++ b/src/User.kt
@@ -13,7 +13,10 @@ class UserService {
     fun processUser(user: User?) {
-        println(user.name)
+        if (user == null) {
+            throw IllegalArgumentException("User cannot be null")
+        }
+        println(user.name)
     }
 }
```

### Guidelines:

1. **Use standard unified diff format** - Must be parseable by standard diff tools
2. **Include context lines** - Show 3 lines of context before and after changes
3. **Accurate line numbers** - Ensure @@ headers have correct line numbers
4. **Complete hunks** - Each hunk should be self-contained and applicable
5. **One fix per patch** - Separate different fixes into different diff blocks
6. **Priority order** - Start with critical/high severity issues
7. **Maximum 5 patches** - Focus on the most important fixes

**IMPORTANT**:
- Each diff MUST be in a ```diff code block
- Use exact line numbers from the original code
- Include enough context for patch to be applied correctly
- DO NOT use any tools - all code is provided above
""".trimIndent()

    val ZH = """
# 代码修复生成 - 统一差异格式

为分析中识别的关键问题生成 **统一差异补丁**。

## 原始代码

${'$'}{codeContent}

## Lint 问题

${'$'}{lintResults}

## AI 分析

${'$'}{analysisOutput}

## 你的任务

为最关键的问题生成 **统一差异补丁**。使用标准的统一差异格式。

### 必需格式：

对于每个修复，提供简要说明，然后是差异补丁：

#### 修复 #{编号}: {简要标题}
**问题**: {一行描述}
**位置**: {文件}:{行号}

```diff
diff --git a/{文件路径} b/{文件路径}
index {旧哈希}..{新哈希} {模式}
--- a/{文件路径}
+++ b/{文件路径}
@@ -{旧起始},{旧计数} +{新起始},{新计数} @@ {上下文}
 {上下文行}
-{删除的行}
+{添加的行}
 {上下文行}
```

### 示例：

#### 修复 #1: 修复空指针异常
**问题**: 缺少用户参数的空检查
**位置**: src/User.kt:15

```diff
diff --git a/src/User.kt b/src/User.kt
index abc1234..def5678 100644
--- a/src/User.kt
+++ b/src/User.kt
@@ -13,7 +13,10 @@ class UserService {
     fun processUser(user: User?) {
-        println(user.name)
+        if (user == null) {
+            throw IllegalArgumentException("User cannot be null")
+        }
+        println(user.name)
     }
 }
```

### 指南：

1. **使用标准统一差异格式** - 必须可被标准差异工具解析
2. **包含上下文行** - 在更改前后显示 3 行上下文
3. **准确的行号** - 确保 @@ 头部有正确的行号
4. **完整的块** - 每个块应该是独立的且可应用的
5. **每个补丁一个修复** - 将不同的修复分成不同的差异块
6. **优先级顺序** - 从关键/高严重性问题开始
7. **最多 5 个补丁** - 专注于最重要的修复

**重要**:
- 每个差异必须在 ```diff 代码块中
- 使用原始代码的确切行号
- 包含足够的上下文以正确应用补丁
- 不要使用任何工具 - 所有代码都在上面提供
""".trimIndent()
}
