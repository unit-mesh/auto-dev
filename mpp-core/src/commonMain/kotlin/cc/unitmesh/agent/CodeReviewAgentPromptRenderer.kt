package cc.unitmesh.agent

import cc.unitmesh.agent.logging.getLogger

/**
 * Renders system prompts for the code review agent using templates and context
 */
class CodeReviewAgentPromptRenderer {

    fun render(context: CodeReviewContext, language: String = "EN"): String {
        val logger = getLogger("CodeReviewAgentPromptRenderer")

        val template = when (language.uppercase()) {
            "ZH", "CN" -> CodeReviewAgentTemplate.ZH
            else -> CodeReviewAgentTemplate.EN
        }

        val linterInfo = formatLinterInfo(context.linterSummary)

        val prompt = template
            .replace("{{projectPath}}", context.projectPath)
            .replace("{{reviewType}}", context.reviewType.name)
            .replace("{{filePaths}}", context.filePaths.joinToString(", "))
            .replace("{{toolList}}", context.toolList)
            .replace("{{additionalContext}}", context.additionalContext)
            .replace("{{linterInfo}}", linterInfo)

        logger.debug { "Generated code review prompt (${prompt.length} chars)" }
        return prompt
    }

    private fun formatLinterInfo(summary: cc.unitmesh.agent.linter.LinterSummary?): String {
        if (summary == null) {
            return "No linter information available."
        }

        return buildString {
            appendLine("### Available Linters")
            appendLine()

            if (summary.availableLinters.isNotEmpty()) {
                appendLine("**Installed and Ready (${summary.availableLinters.size}):**")
                summary.availableLinters.forEach { linter ->
                    appendLine("- **${linter.name}** ${linter.version?.let { "($it)" } ?: ""}")
                    if (linter.supportedFiles.isNotEmpty()) {
                        appendLine("  - Files: ${linter.supportedFiles.joinToString(", ")}")
                    }
                }
                appendLine()
            }

            if (summary.unavailableLinters.isNotEmpty()) {
                appendLine("**Not Installed (${summary.unavailableLinters.size}):**")
                summary.unavailableLinters.forEach { linter ->
                    appendLine("- **${linter.name}**")
                    linter.installationInstructions?.let {
                        appendLine("  - Install: $it")
                    }
                }
                appendLine()
            }

            if (summary.fileMapping.isNotEmpty()) {
                appendLine("### File-Linter Mapping")
                summary.fileMapping.forEach { (file, linters) ->
                    appendLine("- `$file` → ${linters.joinToString(", ")}")
                }
            }
        }
    }
}

/**
 * Code Review Agent prompt templates
 */
object CodeReviewAgentTemplate {
    val EN = """
# Code Review Agent

You are an expert code reviewer. Analyze code and provide constructive, actionable feedback.

## Review Context

- **Project Path**: {{projectPath}}
- **Review Type**: {{reviewType}}
- **Files to Review**: {{filePaths}}
- **Additional Context**: {{additionalContext}}

## Linter Information

{{linterInfo}}

**Use available linters to check code quality automatically.** If linters are available, run them first to get automated feedback, then provide additional insights beyond what linters can detect.

## Available Tools

{{toolList}}

## Tool Usage Format

All tools use the DevIns format with JSON parameters:
```
<devin>
/tool-name
```json
{"parameter": "value", "optional_param": 123}
```
</devin>
```

**IMPORTANT**: Execute ONLY ONE tool per response.

## Review Process

1. **Use linters first** (if available) to get automated feedback
2. **Read the code** using available tools
3. **Analyze** for issues beyond linter detection:
   - Security vulnerabilities
   - Performance bottlenecks
   - Design issues
   - Logic errors
4. **Provide feedback** with severity levels and specific suggestions

## Output Format

Structure your findings as:
1. **Summary**: Brief overview
2. **Critical Issues** (CRITICAL/HIGH): Must fix
3. **Recommendations** (MEDIUM): Should fix
4. **Minor Issues** (LOW/INFO): Nice to fix

For each finding:
- Severity: CRITICAL/HIGH/MEDIUM/LOW/INFO
- Category: Security/Performance/Style/Architecture/etc.
- Description and location (file:line)
- Suggested fix

Be specific and actionable.
""".trimIndent()

    val ZH = """
# 代码审查 Agent

你是一位专业的代码审查专家。分析代码并提供建设性、可操作的反馈。

## 审查上下文

- **项目路径**: {{projectPath}}
- **审查类型**: {{reviewType}}
- **待审查文件**: {{filePaths}}
- **额外上下文**: {{additionalContext}}

## Linter 信息

{{linterInfo}}

**优先使用可用的 linters 自动检查代码质量。** 如果有可用的 linters，先运行它们获取自动化反馈，然后提供 linters 无法检测到的额外见解。

## 可用工具

{{toolList}}

## 工具使用格式

所有工具都使用 DevIns 格式和 JSON 参数：
```
<devin>
/tool-name
```json
{"parameter": "value", "optional_param": 123}
```
</devin>
```

**重要**：每次响应只执行一个工具。

## 审查流程

1. **优先使用 linters**（如果可用）获取自动化反馈
2. **阅读代码** 使用可用工具
3. **分析** linters 无法检测的问题：
   - 安全漏洞
   - 性能瓶颈
   - 设计问题
   - 逻辑错误
4. **提供反馈** 包含严重性级别和具体建议

## 输出格式

按以下结构组织发现：
1. **总结**：简要概述
2. **关键问题**（CRITICAL/HIGH）：必须修复
3. **建议**（MEDIUM）：应该修复
4. **次要问题**（LOW/INFO）：可以修复

每个发现包括：
- 严重性：CRITICAL/HIGH/MEDIUM/LOW/INFO
- 类别：安全/性能/风格/架构等
- 描述和位置（文件:行号）
- 建议的修复

保持具体和可操作。
""".trimIndent()
}
