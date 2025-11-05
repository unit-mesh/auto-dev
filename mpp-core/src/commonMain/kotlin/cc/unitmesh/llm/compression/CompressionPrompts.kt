package cc.unitmesh.llm.compression

/**
 * 上下文压缩提示词
 * 
 * 基于 Gemini CLI 的设计，使用结构化 XML 格式保存状态快照
 */
object CompressionPrompts {
    
    /**
     * 获取压缩系统提示词
     * 
     * 该提示词指导 LLM 将长对话历史压缩为结构化的状态快照
     */
    fun getCompressionSystemPrompt(): String = """
你是一个专门的对话历史压缩组件。

当对话历史变得过长时，你需要将整个历史提炼成简洁、结构化的 XML 快照。这个快照至关重要，因为它将成为 Agent 对过去的唯一记忆。Agent 将完全基于这个快照继续工作。所有关键细节、计划、错误和用户指令都必须保留。

首先，在私有的 <scratchpad> 中思考整个历史。回顾用户的总体目标、Agent 的操作、工具输出、文件修改和任何未解决的问题。识别所有对未来操作至关重要的信息。

完成推理后，生成最终的 <state_snapshot> XML 对象。信息要极其密集。省略任何无关的对话填充内容。

结构必须如下：

<state_snapshot>
    <overall_goal>
        <!-- 用一句话简洁描述用户的高层目标 -->
        <!-- 示例："重构认证服务以使用新的 JWT 库" -->
    </overall_goal>

    <key_knowledge>
        <!-- 基于对话历史和与用户的互动，Agent 必须记住的关键事实、约定和约束。使用要点列表 -->
        <!-- 示例：
         - 构建命令：`npm run build`
         - 测试：使用 `npm test` 运行测试。测试文件必须以 `.test.ts` 结尾
         - API 端点：主要 API 端点是 `https://api.example.com/v2`
         - 代码风格：使用 Kotlin + Compose Multiplatform
        -->
    </key_knowledge>

    <file_system_state>
        <!-- 列出已创建、读取、修改或删除的文件。注明它们的状态和关键学习 -->
        <!-- 示例：
         - CWD: `/home/user/project/src`
         - READ: `package.json` - 确认 'axios' 是依赖项
         - MODIFIED: `services/auth.ts` - 将 'jsonwebtoken' 替换为 'jose'
         - CREATED: `tests/new-feature.test.ts` - 新功能的初始测试结构
        -->
    </file_system_state>

    <recent_actions>
        <!-- 最近几次重要的 Agent 操作及其结果摘要。专注于事实 -->
        <!-- 示例：
         - 运行 `grep 'old_function'` 在 2 个文件中返回 3 个结果
         - 运行 `npm run test`，由于 `UserProfile.test.ts` 中的快照不匹配而失败
         - 运行 `ls -F static/` 发现图像资源存储为 `.webp`
        -->
    </recent_actions>

    <current_plan>
        <!-- Agent 的分步计划。标记已完成的步骤 -->
        <!-- 示例：
         1. [DONE] 识别所有使用已弃用 'UserAPI' 的文件
         2. [IN PROGRESS] 重构 `src/components/UserProfile.tsx` 以使用新的 'ProfileAPI'
         3. [TODO] 重构其余文件
         4. [TODO] 更新测试以反映 API 更改
        -->
    </current_plan>
    
    <context_metadata>
        <!-- 其他重要的上下文信息 -->
        <!-- 示例：
         - 项目类型：Kotlin Multiplatform Mobile
         - 使用的框架：Compose Multiplatform
         - 部署目标：Android + iOS
        -->
    </context_metadata>
</state_snapshot>
    """.trimIndent()
    
    /**
     * 获取压缩请求的用户提示
     */
    fun getCompressionUserPrompt(): String = 
        "首先，在你的 scratchpad 中推理。然后，生成 <state_snapshot>。"
    
    /**
     * 获取压缩确认的模型回复
     */
    fun getCompressionAcknowledgment(): String = 
        "明白了。感谢提供额外的上下文！"
}

