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

**⚠️ CRITICAL: You MUST strictly follow the output format below. Do NOT use any other format.**

Generate a comprehensive summary of the changes in the following format.

**OUTPUT FORMAT REQUIREMENTS**:

**Step 1: Output the start marker (REQUIRED)**
```
<!-- walkthrough_start -->
```

**Step 2: Walkthrough Section (REQUIRED)**
```markdown
## Walkthrough

{Provide 2-3 paragraphs explaining:
1. Why these changes were made (Why)
2. What was changed (What)
3. Key architectural decisions or significant refactorings}
```

**Step 3: Changes Table (REQUIRED)**
```markdown
## Changes

| Cohort |File(s) | Summary |
|---|---|---|
| **{Component Name}** | `{File Path}` | {Concise summary of changes. Focus on business logic and behavior.} |
```

**Step 4: Sequence Diagram (IF APPLICABLE)**
```markdown
## Sequence Diagram(s)

{If changes involve multi-component interaction, user flows, or complex logic, provide a Mermaid diagram.
Omit this section if not applicable.}

\`\`\`mermaid
sequenceDiagram
    actor User
    participant ComponentA
    participant ComponentB
    ...
\`\`\`
```

**Step 5: Output the end marker (REQUIRED)**
```
<!-- walkthrough_end -->
```

**COMPLETE EXAMPLE OUTPUT**:

```markdown
<!-- walkthrough_start -->

## Walkthrough

This change introduces an artifact-centric Code Review System featuring six serializable artifact types. The main goal is to provide a structured review process with support for asynchronous and parallel reviews.

Core improvements include: CodeReviewAgentManager for review orchestration and session lifecycle management, enhanced three-phase review prompts, and comprehensive test coverage. All components support Kotlin Multiplatform.

The key architectural decision was to use a sealed interface design for artifact types, ensuring type safety and extensibility. StateFlow enables reactive state updates, while SupervisorJob provides fault isolation.

## Changes

| Cohort | File(s) | Summary |
|---|---|---|
| **Artifact Model** | `mpp-core/.../CodeReviewArtifact.kt` | Introduces sealed CodeReviewArtifact interface and six data classes: ReviewPlanArtifact, AnalysisSummaryArtifact, VisualProofArtifact, FixSuggestionArtifact, MetricsReportArtifact, IssueTrackingArtifact. Supports kotlinx.serialization and toMarkdown() formatting. |
| **Agent Manager** | `mpp-core/.../CodeReviewAgentManager.kt` | Implements async review execution, session tracking via StateFlow, lifecycle management. Provides submitReview(), submitParallelReviews(), cancelReview() methods. |
| **Templates** | `mpp-core/.../CodeReviewAgentTemplate.kt` | Refactored to three-phase workflow (Strategic Planning, Information Gathering, Analysis Generation). Establishes standardized severity taxonomy (CRITICAL/HIGH/MEDIUM/LOW). |

## Sequence Diagram(s)

\`\`\`mermaid
sequenceDiagram
    actor User
    participant Manager as CodeReviewAgentManager
    participant Agent as CodeReviewAgent
    participant LLM as LLM/Service

    User->>Manager: submitReview(agent, task)
    Manager->>Manager: generateReviewPlan()
    Manager->>Agent: Execute Phase 1
    Agent->>LLM: Request analysis
    LLM-->>Agent: Return findings
    Manager->>Manager: generateFixSuggestions()
    Manager-->>User: Return artifacts
\`\`\`

<!-- walkthrough_end -->
```

## Output Requirements (MUST COMPLY)

1. **Strict Format** - MUST include `<!-- walkthrough_start -->` and `<!-- walkthrough_end -->` markers
2. **Required Sections** - Walkthrough and Changes table are REQUIRED, cannot be omitted
3. **Changes Table** - Group files logically by component when possible
4. **Sequence Diagram** - Only provide when there are multi-component interactions
5. **NO Other Formats** - Do NOT output "Top Priority Issues" list or any other format
6. **Focus on Structure** - Do NOT list low-level code issues (typos, formatting, etc.)

**Validation Checklist**:
- [ ] Output starts with `<!-- walkthrough_start -->`
- [ ] Contains `## Walkthrough` section (2-3 paragraphs)
- [ ] Contains `## Changes` table
- [ ] Output ends with `<!-- walkthrough_end -->`
- [ ] Does NOT use other formats (e.g., "Top Priority Issues")

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

**⚠️ 重要：你必须严格遵循以下输出格式。不要使用任何其他格式。**

按照以下格式生成变更的综合摘要。

**输出格式要求**：

**第一步：必须输出以下标记**
```
<!-- walkthrough_start -->
```

**第二步：Walkthrough 部分（必需）**
```markdown
## Walkthrough

{提供 2-3 段高级摘要，解释：
1. 为什么做这些变更（Why）
2. 变更了什么（What）
3. 关键架构决策或重大重构}
```

**第三步：Changes 表格（必需）**
```markdown
## Changes

| 模块 | 文件 | 摘要 |
|---|---|
| **{组件名称}**  | `{文件路径}` | {该文件中变更的简要摘要。关注业务逻辑和行为变更。} |
```

**第四步：Sequence Diagram（如适用）**
```markdown
## Sequence Diagram(s)

{如果变更涉及多个组件交互、用户流程或复杂逻辑，提供 Mermaid 时序图。
如果不适用，可以省略此部分。}

\`\`\`mermaid
sequenceDiagram
    actor User
    participant ComponentA
    participant ComponentB
    ...
\`\`\`
```

**第五步：必须输出结束标记**
```
<!-- walkthrough_end -->
```

**完整示例输出**：

```markdown
<!-- walkthrough_start -->

## Walkthrough

本次变更引入了基于 artifact 的代码审查系统，包含六种可序列化的 artifact 类型。主要目标是提供结构化的审查流程，支持异步和并行审查。

核心改进包括：CodeReviewAgentManager 用于审查编排和会话生命周期管理，增强的三阶段审查提示，以及全面的测试覆盖。所有组件都支持 Kotlin Multiplatform。

关键架构决策是采用 sealed interface 设计 artifact 类型，确保类型安全和可扩展性。使用 StateFlow 实现响应式状态更新，SupervisorJob 提供故障隔离。

## Changes

| 模块 | 文件 | 摘要 |
|---|---|---|
| **Artifact Model** | `mpp-core/.../CodeReviewArtifact.kt` | 引入 sealed CodeReviewArtifact 接口和六个数据类：ReviewPlanArtifact、AnalysisSummaryArtifact、VisualProofArtifact、FixSuggestionArtifact、MetricsReportArtifact、IssueTrackingArtifact。支持 kotlinx.serialization 和 toMarkdown() 格式化。 |
| **Agent Manager** | `mpp-core/.../CodeReviewAgentManager.kt` | 实现异步审查执行、会话跟踪（StateFlow）、生命周期管理。提供 submitReview()、submitParallelReviews()、cancelReview() 等方法。 |
| **Templates** | `mpp-core/.../CodeReviewAgentTemplate.kt` | 重构为三阶段工作流（战略规划、信息收集、分析生成）。建立标准化严重性分类（CRITICAL/HIGH/MEDIUM/LOW）。 |

## Sequence Diagram(s)

\`\`\`mermaid
sequenceDiagram
    actor User
    participant Manager as CodeReviewAgentManager
    participant Agent as CodeReviewAgent
    participant LLM as LLM/Service

    User->>Manager: submitReview(agent, task)
    Manager->>Manager: generateReviewPlan()
    Manager->>Agent: Execute Phase 1
    Agent->>LLM: Request analysis
    LLM-->>Agent: Return findings
    Manager->>Manager: generateFixSuggestions()
    Manager-->>User: Return artifacts
\`\`\`

<!-- walkthrough_end -->
```

## 输出要求（必须遵守）

1. **严格遵循格式** - 必须包含 `<!-- walkthrough_start -->` 和 `<!-- walkthrough_end -->` 标记
2. **必需部分** - Walkthrough 和 Changes 表格是必需的，不能省略
3. **Changes 表格** - 尽可能按逻辑分组文件，每个组件一行
4. **Sequence Diagram** - 仅在有多组件交互时提供
5. **禁止其他格式** - 不要输出"最高优先级问题"列表或其他格式
6. **专注结构和意图** - 不要列出低级代码问题（拼写、格式化等）

**验证清单**：
- [ ] 输出以 `<!-- walkthrough_start -->` 开始
- [ ] 包含 `## Walkthrough` 部分（2-3 段）
- [ ] 包含 `## Changes` 表格
- [ ] 输出以 `<!-- walkthrough_end -->` 结束
- [ ] 没有使用其他格式（如"最高优先级问题"）
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