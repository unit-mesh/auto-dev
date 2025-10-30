package cc.unitmesh.devins.completion

/**
 * 补全触发类型
 * 
 * 定义了不同的补全触发场景，用于确定应该提供什么类型的补全项
 */
enum class CompletionTriggerType {
    /**
     * 无触发
     */
    NONE,
    
    /**
     * Agent 补全 (@)
     * 例如: @clarify, @code-review
     */
    AGENT,
    
    /**
     * 命令补全 (/)
     * 例如: /file, /symbol, /speckit.test
     */
    COMMAND,
    
    /**
     * 变量补全 ($)
     * 例如: $input, $output
     */
    VARIABLE,
    
    /**
     * 命令值补全 (:)
     * 例如: /file: 之后的路径补全
     */
    COMMAND_VALUE,
    
    /**
     * 代码块补全 (`)
     * 例如: ```kotlin
     */
    CODE_FENCE
}

