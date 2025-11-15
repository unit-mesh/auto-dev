package cc.unitmesh.agent

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.devins.compiler.template.TemplateCompiler

/**
 * Renders system prompts for the code review agent using templates and context
 * 
 * This class implements the unified AgentPromptRenderer interface and uses
 * TemplateCompiler for consistent template processing across all agents
 */
class CodeReviewAgentPromptRenderer : AgentPromptRenderer<CodeReviewContext> {
    val logger = getLogger("CodeReviewAgentPromptRenderer")

    /**
     * Render system prompt from context
     *
     * @param context The code review context
     * @param language Language for the prompt (EN or ZH)
     * @return The rendered system prompt
     */
    override fun render(context: CodeReviewContext, language: String): String {
        val template = when (language.uppercase()) {
            "ZH", "CN" -> CodeReviewAgentTemplate.ZH
            else -> CodeReviewAgentTemplate.EN
        }

        val variableTable = context.toVariableTable()
        val compiler = TemplateCompiler(variableTable)
        val prompt = compiler.compile(template)

        logger.debug { "Generated code review prompt (${prompt.length} chars)" }
        logger.info { "System Prompt: $prompt" }
        return prompt
    }

    /**
     * Renders a data-driven analysis prompt (for UI scenarios where data is pre-collected)
     * This prompt focuses on analyzing provided data rather than using tools
     */
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
        variableTable.addVariable("fileCount", cc.unitmesh.devins.compiler.variable.VariableType.STRING, filePaths.size.toString())
        variableTable.addVariable("filePaths", cc.unitmesh.devins.compiler.variable.VariableType.STRING, filePaths.joinToString("\n- ", prefix = "- "))
        variableTable.addVariable("codeContent", cc.unitmesh.devins.compiler.variable.VariableType.STRING, formattedFiles)
        variableTable.addVariable("lintResults", cc.unitmesh.devins.compiler.variable.VariableType.STRING, formattedLintResults)
        variableTable.addVariable("diffContext", cc.unitmesh.devins.compiler.variable.VariableType.STRING, if (diffContext.isNotBlank()) "\n\n### Diff Context\n$diffContext" else "")

        val compiler = TemplateCompiler(variableTable)
        val prompt = compiler.compile(template)

        logger.debug { "Generated analysis prompt (${prompt.length} chars)" }
        return prompt
    }

    /**
     * Renders intent analysis prompt (tool-driven approach for analyzing commit intent)
     * This prompt guides the agent to use tools for understanding user intent
     */
    fun renderIntentAnalysisPrompt(
        context: IntentAnalysisContext,
        language: String = "EN"
    ): String {
        val template = when (language.uppercase()) {
            "ZH", "CN" -> IntentAnalysisTemplate.ZH
            else -> IntentAnalysisTemplate.EN
        }

        val variableTable = context.toVariableTable()
        val compiler = TemplateCompiler(variableTable)
        val prompt = compiler.compile(template)

        logger.debug { "Generated intent analysis prompt (${prompt.length} chars)" }
        return prompt
    }

    /**
     * Renders intent analysis prompt with pre-collected data (data-driven approach)
     * Similar to renderAnalysisPrompt but focused on intent analysis with mermaid diagrams
     */
    fun renderIntentAnalysisWithData(
        commitMessage: String,
        commitId: String,
        codeChanges: Map<String, String>,
        diffContext: String = "",
        language: String = "EN"
    ): String {
        val template = when (language.uppercase()) {
            "ZH", "CN" -> IntentAnalysisDataDrivenTemplate.ZH
            else -> IntentAnalysisDataDrivenTemplate.EN
        }

        val formattedChanges = codeChanges.entries.joinToString("\n\n") { (path, content) ->
            """### File: $path
```diff
$content
```"""
        }

        val variableTable = cc.unitmesh.devins.compiler.variable.VariableTable()
        variableTable.addVariable("commitMessage", cc.unitmesh.devins.compiler.variable.VariableType.STRING, commitMessage)
        variableTable.addVariable("commitId", cc.unitmesh.devins.compiler.variable.VariableType.STRING, commitId)
        variableTable.addVariable("codeChanges", cc.unitmesh.devins.compiler.variable.VariableType.STRING, formattedChanges)
        variableTable.addVariable("diffContext", cc.unitmesh.devins.compiler.variable.VariableType.STRING, if (diffContext.isNotBlank()) "\n\n### Additional Context\n$diffContext" else "")

        val compiler = TemplateCompiler(variableTable)
        val prompt = compiler.compile(template)

        logger.debug { "Generated data-driven intent analysis prompt (${prompt.length} chars)" }
        return prompt
    }
}

/**
 * Code Review Agent prompt templates (Tool-driven approach)
 * Use this when the agent should use tools to gather information
 */
object CodeReviewAgentTemplate {
    val EN = """
# Code Review Agent

You are an expert code reviewer. Analyze code and provide constructive, actionable feedback.

## Available Tools

${'$'}{toolList}

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

1. **Analyze linter results** (if provided in user message) to understand existing issues
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

## 可用工具

${'$'}{toolList}

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

1. **分析 linter 结果**（如果在用户消息中提供）理解已有问题
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

---

## Analysis Guidelines

1. **TWO-PART OUTPUT**: 
   - Part 1 (Console Summary): 3-5 critical/high issues only, very brief
   - Part 2 (Full Report): Complete analysis of up to 10 issues with details
2. **Prioritize by severity** (Use strict criteria):
   - **CRITICAL**: ONLY for issues that WILL cause security breaches, data loss, or system crashes
     - Examples: SQL injection, exposed secrets, null pointer dereferences in critical paths
   - **HIGH**: Issues that WILL cause incorrect behavior or significant performance degradation
     - Examples: Logic errors with wrong results, resource leaks, race conditions
   - **MEDIUM**: Issues that MAY cause problems under certain conditions
     - Examples: Missing error handling, suboptimal algorithms, missing validations
   - **LOW/INFO**: Code quality issues that don't affect functionality
     - Examples: Code duplication, minor style inconsistencies, missing comments

## Output Requirements

- Use proper Markdown formatting
- Start with Summary, then list exactly 10 issues (or fewer if less than 10 significant issues exist)
- Number issues from 1-10
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
 * Intent Analysis prompt templates (Tool-driven approach)
 * Used for analyzing commit intent and related issues
 */
object IntentAnalysisTemplate {
    val EN = """
# Commit Intent Analysis Agent

You are an expert software analyst. Your task is to analyze commits and understand the developer's intent.

## Available Tools

${'$'}{toolList}

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

## Analysis Process

1. **Understand the commit context**:
   - Review the commit message and code changes provided by the user
   - Identify related issues/tickets mentioned in the commit

2. **Gather additional context** (use tools as needed):
   - Read relevant source files to understand the codebase structure
   - Read test files to understand expected behavior
   - Read related files mentioned in the changes
   - Search for related code patterns using grep

3. **Analyze user intent**:
   - What problem is the developer trying to solve?
   - What is the intended behavior or feature?
   - How does this relate to the mentioned issues/tickets?

4. **Create visualization**:
   - Generate a Mermaid diagram showing:
     * User's intent/goal
     * Implementation approach
     * Data flow or component interactions
     * Key decision points

5. **Evaluate implementation**:
   - Does the implementation match the stated intent?
   - Are there any gaps or inconsistencies?
   - Are there potential issues or improvements?

## Output Format

---

## Console Summary (Keep Brief)

### 🎯 Intent Summary
One sentence: What the developer intended to achieve.

### 📊 Mermaid Diagram
```mermaid
graph TD
    A[Intent] --> B[Implementation]
    B --> C[Outcome]
```

## Guidelines

- Use tools to read files and understand context
- Be specific and reference actual code/files
- Focus on understanding WHY the changes were made, not just WHAT changed
- Provide actionable insights for improvement
- Keep the mermaid diagram clear and focused on intent flow
""".trimIndent()

    val ZH = """
# 提交意图分析 Agent

你是一位专业的软件分析专家。你的任务是分析提交并理解开发者的意图。

## 可用工具

${'$'}{toolList}

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

## 分析流程

1. **理解提交上下文**：
   - 审查用户提供的提交消息和代码更改
   - 识别提交中提到的相关问题/工单

2. **收集额外上下文**（根据需要使用工具）：
   - 读取相关源文件以理解代码库结构
   - 读取测试文件以理解预期行为
   - 读取更改中提到的相关文件
   - 使用 grep 搜索相关代码模式

3. **分析用户意图**：
   - 开发者试图解决什么问题？
   - 预期的行为或功能是什么？
   - 这与提到的问题/工单有何关系？

4. **创建可视化**：
   - 生成 Mermaid 图表显示：
     * 用户的意图/目标
     * 实现方法
     * 数据流或组件交互
     * 关键决策点

5. **评估实现**：
   - 实现是否符合声明的意图？
   - 是否存在任何差距或不一致？
   - 是否有潜在问题或改进空间？

## 输出格式

---

## 控制台摘要（保持简短）

### 🎯 意图总结
一句话：开发者意图实现的目标。

### 📊 Mermaid 图表
```mermaid
graph TD
    A[意图] --> B[实现]
    B --> C[结果]
```

### ✅ 快速评估
- **准确性**：高/中/低
- **关键问题**：（如有，一句话）

---

## 指南

- 使用工具读取文件并理解上下文
- 具体说明并引用实际代码/文件
- 专注于理解为什么进行更改，而不仅仅是更改了什么
- 提供可操作的改进见解
- 保持 mermaid 图表清晰并专注于意图流程
""".trimIndent()
}

/**
 * Intent Analysis Data-Driven Templates
 * Used when code changes are pre-collected (no tool usage)
 */
object IntentAnalysisDataDrivenTemplate {
    val EN = """
# Commit Intent Analysis

You are an expert software analyst. Analyze the commit to understand the developer's intent.

## Commit Information

**Commit ID**: ${'$'}{commitId}

**Commit Message**:
```
${'$'}{commitMessage}
```

## Code Changes

${'$'}{codeChanges}
${'$'}{diffContext}

## Your Task

Analyze the developer's intent and provide:

### 🎯 Intent Summary
Brief summary of what the developer intended to achieve (2-3 sentences).

### 🔍 Detailed Analysis
- **Primary Goal**: What is the main objective?
- **Implementation Approach**: How is the intent being implemented?
- **Key Changes**: What are the most important changes?

### 📊 Intent Flow Diagram

Create a mermaid diagram to visualize the intent and implementation:

```mermaid
graph TD
    A[User Intent] --> B[Implementation Step 1]
    B --> C[Implementation Step 2]
    C --> D[Expected Outcome]
    %% Add more details about the flow
```

### ✅ Implementation Evaluation
- **Accuracy**: Does the code match the stated intent? (Rate: High/Medium/Low)
- **Completeness**: Are all aspects of the intent addressed?
- **Potential Issues**: List any problems or gaps you notice
- **Suggested Improvements**: Recommendations for better implementation

### 💡 Additional Insights
Any other relevant observations or recommendations.

## Guidelines

- Be specific and reference actual code from the changes
- Focus on understanding WHY the changes were made, not just WHAT changed
- The mermaid diagram should clearly show the flow from intent to implementation
- Provide actionable insights for improvement

**DO NOT** attempt to use any tools. All information is provided above.
""".trimIndent()

    val ZH = """
# 提交意图分析

你是一位专业的软件分析专家。分析提交以理解开发者的意图。

## 提交信息

**Commit ID**: ${'$'}{commitId}

**提交消息**:
```
${'$'}{commitMessage}
```

## 代码更改

${'$'}{codeChanges}
${'$'}{diffContext}

## 你的任务

分析开发者的意图并提供：

### 🎯 意图总结
简要总结开发者意图实现的目标（2-3 句话）。

### 🔍 详细分析
- **主要目标**：主要目的是什么？
- **实现方法**：如何实现意图？
- **关键更改**：最重要的更改是什么？

### 📊 意图流程图

创建 mermaid 图表来可视化意图和实现：

```mermaid
graph TD
    A[用户意图] --> B[实现步骤 1]
    B --> C[实现步骤 2]
    C --> D[预期结果]
    %% 添加更多关于流程的详细信息
```

### ✅ 实现评估
- **准确性**：代码是否符合声明的意图？（评级：高/中/低）
- **完整性**：是否涵盖了意图的所有方面？
- **潜在问题**：列出你注意到的任何问题或差距
- **改进建议**：改善实现的建议

### 💡 其他见解
任何其他相关的观察或建议。

## 指南

- 具体说明并引用更改中的实际代码
- 专注于理解为什么进行更改，而不仅仅是更改了什么
- mermaid 图表应该清楚地显示从意图到实现的流程
- 提供可操作的改进见解

**不要** 尝试使用任何工具。所有信息都已在上面提供。
""".trimIndent()
}
