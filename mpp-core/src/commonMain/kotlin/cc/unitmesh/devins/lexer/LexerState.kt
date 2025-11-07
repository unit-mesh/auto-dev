package cc.unitmesh.devins.lexer

/**
 * 词法分析器状态枚举
 * 对应原始 Flex 词法分析器中的状态
 */
enum class LexerState {
    /**
     * 初始状态
     */
    INITIAL,
    
    /**
     * 已使用状态（处理 @, /, $ 等特殊字符）
     */
    USED,
    
    /**
     * Agent 块状态（处理 @agent）
     */
    AGENT_BLOCK,
    
    /**
     * 变量块状态（处理 $variable）
     */
    VARIABLE_BLOCK,
    
    /**
     * 命令块状态（处理 /command）
     */
    COMMAND_BLOCK,
    
    /**
     * 单行注释块状态
     */
    SINGLE_COMMENT_BLOCK,
    
    /**
     * 命令值块状态
     */
    COMMAND_VALUE_BLOCK,
    
    /**
     * 表达式块状态
     */
    EXPR_BLOCK,
    
    /**
     * 代码块状态
     */
    CODE_BLOCK,
    
    /**
     * 内容注释块状态
     */
    CONTENT_COMMENT_BLOCK,
    
    /**
     * 行块状态
     */
    LINE_BLOCK,
    
    /**
     * 前置元数据块状态
     */
    FRONT_MATTER_BLOCK,
    
    /**
     * 前置元数据值块状态
     */
    FRONT_MATTER_VALUE_BLOCK,
    
    /**
     * 前置元数据值对象状态
     */
    FRONT_MATTER_VAL_OBJECT,
    
    /**
     * 模式动作块状态
     */
    PATTERN_ACTION_BLOCK,
    
    /**
     * 条件表达式块状态
     */
    CONDITION_EXPR_BLOCK,
    
    /**
     * 函数声明块状态
     */
    FUNCTION_DECL_BLOCK,
    
    /**
     * 外部函数块状态
     */
    EXT_FUNCTION_BLOCK,
    
    /**
     * 语言 ID 状态
     */
    LANG_ID
}

/**
 * 词法分析器上下文
 * 管理词法分析过程中的状态和标志
 */
data class LexerContext(
    /**
     * 当前状态
     */
    var currentState: LexerState = LexerState.INITIAL,
    
    /**
     * 状态栈，用于状态的嵌套和恢复
     */
    val stateStack: MutableList<LexerState> = mutableListOf(),
    
    /**
     * 是否在代码块开始状态
     */
    var isCodeStart: Boolean = false,
    
    /**
     * 是否在 DevIn 模板内部
     */
    var isInsideDevInTemplate: Boolean = false,
    
    /**
     * 是否在函数块内部
     */
    var isInsideFunctionBlock: Boolean = false,
    
    /**
     * 是否在前置元数据内部
     */
    var isInsideFrontMatter: Boolean = false,
    
    /**
     * 是否有前置元数据
     */
    var hasFrontMatter: Boolean = false,
    
    /**
     * 模式动作大括号是否开始
     */
    var patternActionBraceStart: Boolean = false,
    
    /**
     * 模式动作大括号嵌套级别
     */
    var patternActionBraceLevel: Int = 0,
    
    /**
     * 上一个字符（用于判断是否应该识别特殊字符）
     * 修复：只在行首或空白后识别 @/$/#
     */
    var lastChar: Char? = null,
    
    /**
     * 当前行是否在行首
     */
    var isAtLineStart: Boolean = true
) {
    /**
     * 推入状态到栈中
     */
    fun pushState(state: LexerState) {
        stateStack.add(currentState)
        currentState = state
    }
    
    /**
     * 从栈中弹出状态
     */
    fun popState(): LexerState? {
        return if (stateStack.isNotEmpty()) {
            val previousState = currentState
            currentState = stateStack.removeAt(stateStack.size - 1)
            previousState
        } else {
            null
        }
    }
    
    /**
     * 切换到新状态
     */
    fun switchTo(state: LexerState) {
        currentState = state
    }
    
    /**
     * 重置上下文
     */
    fun reset() {
        currentState = LexerState.INITIAL
        stateStack.clear()
        isCodeStart = false
        isInsideDevInTemplate = false
        isInsideFunctionBlock = false
        isInsideFrontMatter = false
        hasFrontMatter = false
        patternActionBraceStart = false
        patternActionBraceLevel = 0
        lastChar = null
        isAtLineStart = true
    }
    
    /**
     * 记录刚处理的字符（用于上下文判断）
     */
    fun recordChar(char: Char) {
        lastChar = char
        if (char == '\n') {
            isAtLineStart = true
        } else if (!char.isWhitespace()) {
            isAtLineStart = false
        }
    }
    
    /**
     * 检查是否应该识别特殊字符（@/$/#）
     * 只在行首或上一个字符是空白时才识别
     */
    fun shouldRecognizeSpecialChar(): Boolean {
        return isAtLineStart || lastChar == null || lastChar!!.isWhitespace()
    }
    
    /**
     * 复制上下文
     */
    fun copy(): LexerContext {
        return LexerContext(
            currentState = currentState,
            stateStack = stateStack.toMutableList(),
            isCodeStart = isCodeStart,
            isInsideDevInTemplate = isInsideDevInTemplate,
            isInsideFunctionBlock = isInsideFunctionBlock,
            isInsideFrontMatter = isInsideFrontMatter,
            hasFrontMatter = hasFrontMatter,
            patternActionBraceStart = patternActionBraceStart,
            patternActionBraceLevel = patternActionBraceLevel,
            lastChar = lastChar,
            isAtLineStart = isAtLineStart
        )
    }
}
