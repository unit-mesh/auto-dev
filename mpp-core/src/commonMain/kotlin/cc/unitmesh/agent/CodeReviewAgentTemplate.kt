package cc.unitmesh.agent

object CodeReviewAnalysisTemplate {
    val EN = """
# Code Review Analysis

You are an expert code reviewer. Analyze the provided code and linter results to identify the **TOP 10 HIGHEST PRIORITY** issues.

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