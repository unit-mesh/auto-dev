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

${if (Platform.isJvm) """
{If changes involve multi-component interaction, user flows, or complex logic, provide a PlantUML diagram.
Omit this section if not applicable.}

\`\`\`plantuml
@startuml
actor User
participant "ComponentA" as A
participant "ComponentB" as B
User -> A: action
A -> B: request
B --> A: response
A --> User: result
@enduml
\`\`\`
""" else """
{If changes involve multi-component interaction, user flows, or complex logic, provide a Mermaid diagram.
Omit this section if not applicable.}

\`\`\`mermaid
sequenceDiagram
    actor User
    participant ComponentA
    participant ComponentB
    ...
\`\`\`
"""}
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

${if (Platform.isJvm) """
\`\`\`plantuml
@startuml
actor User
participant "CodeReviewAgentManager" as Manager
participant "CodeReviewAgent" as Agent
participant "LLM/Service" as LLM

User -> Manager: submitReview(agent, task)
Manager -> Manager: generateReviewPlan()
Manager -> Agent: Execute Phase 1
Agent -> LLM: Request analysis
LLM --> Agent: Return findings
Manager -> Manager: generateFixSuggestions()
Manager --> User: Return artifacts
@enduml
\`\`\`
""" else """
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
"""}

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

${if (Platform.isJvm) """
{如果变更涉及多个组件交互、用户流程或复杂逻辑，提供 PlantUML 时序图。
如果不适用，可以省略此部分。}

\`\`\`plantuml
@startuml
actor User
participant "ComponentA" as A
participant "ComponentB" as B
User -> A: action
A -> B: request
B --> A: response
A --> User: result
@enduml
\`\`\`
""" else """
{如果变更涉及多个组件交互、用户流程或复杂逻辑，提供 Mermaid 时序图。
如果不适用，可以省略此部分。}

\`\`\`mermaid
sequenceDiagram
    actor User
    participant ComponentA
    participant ComponentB
    ...
\`\`\`
"""}
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

${if (Platform.isJvm) """
\`\`\`plantuml
@startuml
actor User
participant "CodeReviewAgentManager" as Manager
participant "CodeReviewAgent" as Agent
participant "LLM/Service" as LLM

User -> Manager: submitReview(agent, task)
Manager -> Manager: generateReviewPlan()
Manager -> Agent: Execute Phase 1
Agent -> LLM: Request analysis
LLM --> Agent: Return findings
Manager -> Manager: generateFixSuggestions()
Manager --> User: Return artifacts
@enduml
\`\`\`
""" else """
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
"""}

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

## 🚨 PRIORITY RULES

**ABSOLUTE PRIORITY: Fix files with ERRORS first!**

1. **🔴 ERRORS (CRITICAL)** - Files with compilation/lint errors MUST be fixed first
   - These will break the build or cause runtime failures
   - Fix ALL errors in a file before moving to warnings
   
2. **⚠️ WARNINGS (LOWER PRIORITY)** - Only fix after all errors are resolved
   - These are best practices or potential issues
   - Can be skipped if time/complexity is high

## Important Constraints

1. **ONE PATCH PER FILE** - If a file has multiple issues, combine all fixes into a single unified diff
2. **Only fix changed code** - Focus ONLY on the code blocks shown below (user's changes)
3. **Maximum 5 files** - Start with error files, then warnings if space permits
4. **Use exact line numbers** - Line numbers must match the code blocks provided
5. **ERRORS FIRST** - Always prioritize files marked with 🚨 CRITICAL PRIORITY

## Changed Code Blocks (User's Changes)

${'$'}{changedCode}

## Lint Issues (Prioritized by Severity)

${'$'}{lintResults}

## AI Analysis (Issues found in Phase 1)

${'$'}{analysisOutput}

## Your Task

**Step 1: Identify priority files**
- Look for files marked with "🚨 CRITICAL PRIORITY" or "❌" (errors)
- These MUST be fixed first

**Step 2: Generate fixes in priority order**

For each file that needs fixes:

1. Identify all issues in that file from the analysis
2. **Combine** all fixes for that file into **ONE** unified diff patch
3. Ensure the patch applies cleanly to the changed code blocks above
4. Start with ERROR files, then WARNING files if you have remaining slots

### Required Format:

#### Fix for {filepath}

**Issues addressed**:
- {Issue 1 description} (Line {X})
- {Issue 2 description} (Line {Y})

```diff
diff --git a/{filepath} b/{filepath}
index {old_hash}..{new_hash} {mode}
--- a/{filepath}
+++ b/{filepath}
@@ -{old_start},{old_count} +{new_start},{new_count} @@
 {context line}
-{removed line}
+{added line}
 {context line}
```

### Example (Multiple fixes in one file):

#### Fix for src/User.kt

**Issues addressed**:
- Missing null check for user parameter (Line 15)
- Potential memory leak - close resource (Line 45)

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
@@ -43,6 +46,7 @@ class UserService {
     fun loadData() {
         val stream = openStream()
         // ... process stream
+        stream.close()
     }
 }
```

### Guidelines:

1. **🚨 ERROR FILES FIRST** - Always fix files with errors before warnings
2. **ONE unified diff per file** - Combine multiple fixes for the same file
3. **Use standard unified diff format** - Must be parseable by standard diff tools
4. **Include context lines** - Show 3 lines of context before and after changes
5. **Accurate line numbers** - Ensure @@ headers have correct line numbers
6. **Complete hunks** - Each hunk should be self-contained and applicable
7. **Maximum 5 files** - Prioritize error files, then warnings if space permits

**CRITICAL RULES**:
- 🔴 **ERRORS FIRST** - Files with errors have absolute priority
- ✅ ONE unified diff per file (combine multiple fixes)
- ✅ Only modify code in the "Changed Code Blocks" section
- ✅ Include 3 lines of context before/after each change
- ⚠️ Warnings can be skipped if all 5 slots are used by error fixes
- ❌ DO NOT use any tools
- ❌ DO NOT generate multiple patches for the same file

**Example Priority:**
```
Fix order:
1. file_with_2_errors.kt (🔴 CRITICAL)
2. file_with_1_error.kt (🔴 CRITICAL)
3. file_with_error_and_warnings.kt (🔴 CRITICAL)
4. file_with_warnings_only.kt (⚠️ Lower priority)
5. another_warnings_file.kt (⚠️ Lower priority)
```
""".trimIndent()

    val ZH = """
# 代码修复生成 - 统一差异格式

为分析中识别的关键问题生成 **统一差异补丁**。

## 🚨 优先级规则

**绝对优先：先修复有 ERROR 的文件！**

1. **🔴 ERRORS（关键）** - 有编译/lint 错误的文件必须优先修复
   - 这些会导致构建失败或运行时错误
   - 先修复文件中的所有错误，再考虑警告
   
2. **⚠️ WARNINGS（较低优先级）** - 只在所有错误解决后修复
   - 这些是最佳实践或潜在问题
   - 如果时间/复杂度高可以跳过

## 重要约束

1. **每个文件一个补丁** - 如果一个文件有多个问题，将所有修复合并到一个统一差异中
2. **只修复改动的代码** - 只关注下面显示的代码块（用户的改动）
3. **最多 5 个文件** - 从错误文件开始，如果有空间再修复警告
4. **使用精确的行号** - 行号必须与提供的代码块匹配
5. **错误优先** - 始终优先处理标记为 🚨 关键优先级的文件

## 改动的代码块（用户的改动）

${'$'}{changedCode}

## Lint 问题（按严重性优先级排序）

${'$'}{lintResults}

## AI 分析（第一阶段发现的问题）

${'$'}{analysisOutput}

## 你的任务

**步骤 1：识别优先级文件**
- 查找标记为"🚨 关键优先级"或"❌"（错误）的文件
- 这些必须优先修复

**步骤 2：按优先级顺序生成修复**

对于每个需要修复的文件：

1. 从分析中识别该文件的所有问题
2. **合并**该文件的所有修复到**一个**统一差异补丁中
3. 确保补丁可以干净地应用到上面的改动代码块
4. 从 ERROR 文件开始，如果还有剩余位置再处理 WARNING 文件

### 必需格式：

#### 修复文件 {文件路径}

**解决的问题**:
- {问题1描述} (第 {X} 行)
- {问题2描述} (第 {Y} 行)

```diff
diff --git a/{文件路径} b/{文件路径}
index {旧哈希}..{新哈希} {模式}
--- a/{文件路径}
+++ b/{文件路径}
@@ -{旧起始},{旧计数} +{新起始},{新计数} @@
 {上下文行}
-{删除的行}
+{添加的行}
 {上下文行}
```

### 示例（一个文件中的多个修复）：

#### 修复文件 src/User.kt

**解决的问题**:
- 缺少用户参数的空检查 (第 15 行)
- 潜在的内存泄漏 - 关闭资源 (第 45 行)

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
@@ -43,6 +46,7 @@ class UserService {
     fun loadData() {
         val stream = openStream()
         // ... process stream
+        stream.close()
     }
 }
```

### 指南：

1. **🚨 错误文件优先** - 始终先修复有错误的文件，再修复警告
2. **每个文件一个统一差异** - 合并同一文件的多个修复
3. **使用标准统一差异格式** - 必须可被标准差异工具解析
4. **包含上下文行** - 在更改前后显示 3 行上下文
5. **准确的行号** - 确保 @@ 头部有正确的行号
6. **完整的块** - 每个块应该是独立的且可应用的
7. **最多 5 个文件** - 优先错误文件，如果有空间再处理警告

**关键规则**:
- 🔴 **错误优先** - 有错误的文件拥有绝对优先级
- ✅ 每个文件一个统一差异（合并多个修复）
- ✅ 只修改"改动的代码块"部分中的代码
- ✅ 在每个更改前后包含 3 行上下文
- ⚠️ 如果所有 5 个位置都被错误修复占用，警告可以跳过
- ❌ 不要使用任何工具
- ❌ 不要为同一文件生成多个补丁

**优先级示例：**
```
修复顺序：
1. file_with_2_errors.kt (🔴 关键)
2. file_with_1_error.kt (🔴 关键)
3. file_with_error_and_warnings.kt (🔴 关键)
4. file_with_warnings_only.kt (⚠️ 较低优先级)
5. another_warnings_file.kt (⚠️ 较低优先级)
```
""".trimIndent()
}

/**
 * Template for modification plan generation
 * Generates concise, structured modification suggestions using Plan-like format
 */
object ModificationPlanTemplate {
    val EN = """
# Modification Plan Generation

Based on the code analysis and lint results, provide a **specific, actionable modification plan** using the Plan format.

## Analysis Context

${'$'}{analysisOutput}

## Lint Issues Summary

${'$'}{lintResults}

## Your Task

Generate a **specific** modification plan with **maximum 5 items**, using the Plan markdown format with nested structure.

### Required Format:

Your output MUST be inside a code block with language `plan`:

```plan
1. {Issue Category} - {Priority}
    - [ ] Fix [FileName](filepath) line {X}: {Specific issue}
    - [ ] {Why this fix is needed}
    - [ ] {How to fix: concrete action}

2. {Issue Category} - {Priority}
    - [ ] Fix [FileName](filepath) line {Y}: {Specific issue}
    - [ ] {Why}
    - [ ] {How}
```

### Key Requirements:

1. **Maximum 5 items** - Focus on most critical errors (prioritize ERROR > WARNING)
2. **Must be specific** - Each item must mention specific file names using [FileName](filepath) format
3. **Priority**: CRITICAL (ERROR) | HIGH (WARNING) | MEDIUM (Code quality)
4. **Actionable** - "How" should provide concrete methods, not vague statements
5. **Based on Lint** - Must reference actual errors from Lint results above
6. **Use Plan format** - Ordered list for main items, unordered list for details
7. **File links** - Use [FileName](filepath) format for file references

### Good Examples:

```plan
1. Null Safety Issues - CRITICAL
    - [ ] Fix [UserService.kt](src/main/kotlin/UserService.kt) line 45: user parameter not null-checked
    - [ ] Prevents NullPointerException (detekt UseCheckOrError rule)
    - [ ] Add requireNotNull(user) at line 45 or use safe call operator ?.

2. Exception Handling - CRITICAL
    - [ ] Fix [DatabaseHelper.kt](src/main/kotlin/DatabaseHelper.kt) line 62: catching overly generic Exception
    - [ ] May hide specific errors (detekt TooGenericExceptionCaught)
    - [ ] Catch specific exceptions like SQLException or IOException instead

3. Resource Management - HIGH
    - [ ] Fix [FileProcessor.kt](src/main/kotlin/FileProcessor.kt) line 78: stream not closed
    - [ ] Potential memory leak (detekt UnclosedResource)
    - [ ] Use use() block or try-with-resources pattern
```

### Bad Examples (DO NOT do this):

```plan
❌ 1. Code Quality Improvement - HIGH
❌     - [ ] Optimize code structure
❌     - [ ] Improve maintainability
❌     - [ ] Refactor code

(Problems: No file name, no specific error, not actionable, no file links)
```

**STRICT RULES**:
- ✅ Maximum 5 items (no more)
- ✅ Use `plan` code block format
- ✅ Each item must include [FileName](filepath) with actual file path
- ✅ Must reference actual errors from Lint results
- ✅ Priority based on ERROR/WARNING severity
- ✅ Merge multiple similar errors in same file into one item
- ✅ Use nested structure: ordered list for items, unordered list for details
- ❌ DO NOT use vague descriptions (like "optimize code")
- ❌ DO NOT provide code examples in the plan
- ❌ DO NOT use tools
- ❌ DO NOT use emojis in the plan content
""".trimIndent()

    val ZH = """
# 修改计划生成

基于代码分析和 Lint 结果，提供**具体、可执行的修改计划**，使用 Plan 格式。

## 分析上下文

${'$'}{analysisOutput}

## Lint 问题摘要

${'$'}{lintResults}

## 你的任务

生成**具体**修改计划，**最多 5 项**，使用 Plan markdown 格式和嵌套结构。

### 必需格式：

你的输出必须在 `plan` 代码块中：

```plan
1. {问题类别} - {优先级}
    - [ ] 修复 [文件名](文件路径) 第 {X} 行: {具体问题}
    - [ ] {为什么需要修复}
    - [ ] {如何修复: 具体操作}

2. {问题类别} - {优先级}
    - [ ] 修复 [文件名](文件路径) 第 {Y} 行: {具体问题}
    - [ ] {原因}
    - [ ] {方法}
```

### 关键要求：

1. **最多 5 项** - 聚焦最关键的错误（优先 ERROR > WARNING）
2. **必须具体** - 每项必须使用 [文件名](文件路径) 格式提到具体文件
3. **优先级**: 关键（ERROR）| 高（WARNING）| 中等（代码质量）
4. **可执行** - "如何修复"要提供具体方法，不要泛泛而谈
5. **基于 Lint** - 必须引用上面 Lint 结果中的实际错误
6. **使用 Plan 格式** - 有序列表用于主要项，无序列表用于详情
7. **文件链接** - 使用 [文件名](文件路径) 格式引用文件

### 好的示例：

```plan
1. 空安全问题 - 关键
    - [ ] 修复 [UserService.kt](src/main/kotlin/UserService.kt) 第 45 行: user 参数未做空检查
    - [ ] 防止 NullPointerException (detekt UseCheckOrError 规则)
    - [ ] 在第 45 行添加 requireNotNull(user) 或使用安全调用操作符 ?.

2. 异常处理 - 关键
    - [ ] 修复 [DatabaseHelper.kt](src/main/kotlin/DatabaseHelper.kt) 第 62 行: 捕获了过于泛化的 Exception
    - [ ] 可能掩盖具体错误 (detekt TooGenericExceptionCaught)
    - [ ] 改为捕获具体异常类型如 SQLException 或 IOException

3. 资源管理 - 高
    - [ ] 修复 [FileProcessor.kt](src/main/kotlin/FileProcessor.kt) 第 78 行: 流未关闭
    - [ ] 潜在内存泄漏 (detekt UnclosedResource)
    - [ ] 使用 use() 块或 try-with-resources 模式
```

### 坏的示例（不要这样）：

```plan
❌ 1. 代码质量提升 - 高
❌     - [ ] 优化代码结构
❌     - [ ] 提高可维护性
❌     - [ ] 重构代码

（问题：没有文件名、没有具体错误、无法执行、没有文件链接）
```

**严格规则**:
- ✅ 最多 5 项（不能更多）
- ✅ 使用 `plan` 代码块格式
- ✅ 每项必须包含 [文件名](文件路径) 和实际文件路径
- ✅ 必须引用 Lint 结果中的实际错误
- ✅ 优先级根据 ERROR/WARNING 严重性确定
- ✅ 合并同一文件的多个相似错误到一项
- ✅ 使用嵌套结构：有序列表用于项，无序列表用于详情
- ❌ 不要使用泛泛的描述（如"优化代码"）
- ❌ 不要在计划中提供代码示例
- ❌ 不要使用工具
- ❌ 不要在计划内容中使用 emoji
""".trimIndent()
}
