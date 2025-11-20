package cc.unitmesh.agent

object CodeReviewAnalysisTemplate {
    val EN = """
# Code Review - Phase 1: Walkthrough & Summary

You are an expert code reviewer. Your task is to provide a high-level walkthrough of the changes, summarizing the intent, implementation details, and flow.

## Available Tools

You have access to the following tools through DevIns commands. Use these tools to gather additional context when needed:

${'$'}{toolList}

## Tool Usage Format

All tools use the DevIns format with JSON parameters:
<devin>
/tool-name
```json
{"parameter": "value", "optional_param": 123}
```
</devin>

**IMPORTANT: Execute ONE tool at a time**
- ✅ Correct: One <devin> block with one tool call per response
- ❌ Wrong: Multiple <devin> blocks or multiple tools in one response

## Response Format

For each tool call, respond with:
1. Your reasoning about what to do next (explain your thinking)
2. **EXACTLY ONE** DevIns command (wrapped in <devin></devin> tags)
3. What you expect to happen

After gathering all necessary information, provide your final analysis WITHOUT any tool calls.

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

Generate a comprehensive summary of the changes in the following format.

**OUTPUT FORMAT**:

<!-- This is an auto-generated comment: summarize by coderabbit.ai -->
<!-- walkthrough_start -->

## Walkthrough

{Provide a high-level summary of the PR/Changes. Explain the "Why" and "What" of the changes. Mention key architectural decisions, new components, or significant refactorings. Keep it to 2-3 paragraphs.}

## Changes

| Cohort / File(s) | Summary |
|---|---|
| **{Component Name}** <br> `{File Path}` | {Concise summary of the changes in this file. Focus on business logic and behavior changes.} |
| ... | ... |

## Sequence Diagram(s)

{If the changes involve interaction between multiple components, user flows, or complex logic, provide a Mermaid sequence diagram. If not applicable, omit this section or provide a simple class diagram if relevant.}

```mermaid
sequenceDiagram
    actor User
    participant ComponentA
    participant ComponentB
    ...
```

<!-- walkthrough_end -->

## Output Requirements

- **Strictly follow the format above.**
- The "Changes" table should group files logically if possible, or list them individually.
- The Sequence Diagram is highly recommended for feature changes.
- Do NOT list individual low-level code issues (typos, formatting) in this phase. Focus on the *structure* and *intent*.
""".trimIndent()

    val ZH = """
# 代码审查 - 第一阶段：流程与总结

你是一位专业的代码审查专家。你的任务是提供变更的高级流程演练，总结意图、实现细节和流程。

## 可用工具

你可以通过 DevIns 命令访问以下工具。在需要时使用这些工具收集额外的上下文：

${'$'}{toolList}

## 工具使用格式

所有工具都使用 DevIns 格式和 JSON 参数：
<devin>
/tool-name
```json
{"parameter": "value", "optional_param": 123}
```
</devin>

## 重要：每次响应只执行一个工具

**你必须每次响应只执行一个工具。** 不要在单个响应中包含多个工具调用。

- ✅ 正确：一个 <devin> 块包含一个工具调用
- ❌ 错误：多个 <devin> 块或一个块中有多个工具

## 响应格式

对于每个工具调用，请回复：
1. 你对下一步该做什么的推理（解释你的思考）
2. **恰好一个** DevIns 命令（包装在 <devin></devin> 标签中）
3. 你期望发生什么

在收集完所有必要信息后，提供你的最终分析，**不要再包含任何工具调用**。

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

按照以下格式生成变更的综合摘要。

**输出格式**：

<!-- walkthrough_start -->

## Walkthrough

{提供 PR/变更的高级摘要。解释变更的“原因”和“内容”。提及关键架构决策、新组件或重大重构。保持在 2-3 段。}

## Changes

| 模块 / 文件 | 摘要 |
|---|---|
| **{组件名称}** <br> `{文件路径}` | {该文件中变更的简要摘要。关注业务逻辑和行为变更。} |
| ... | ... |

## Sequence Diagram(s)

{如果变更涉及多个组件之间的交互、用户流程或复杂逻辑，请提供 Mermaid 时序图。如果不适用，可以省略此部分或提供简单的类图。}

```mermaid
sequenceDiagram
    actor User
    participant ComponentA
    participant ComponentB
    ...
```

<!-- walkthrough_end -->

## 输出要求

- **严格遵循上述格式。**
- “Changes”表应尽可能按逻辑分组文件，或单独列出。
- 强烈建议对功能变更使用时序图。
- 在此阶段**不要**列出个别的低级代码问题（拼写错误、格式化）。专注于**结构**和**意图**。
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