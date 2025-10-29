package cc.unitmesh.devins.ast

import cc.unitmesh.devins.token.DevInsToken
import cc.unitmesh.devins.token.TokenPosition

/**
 * DevIns 文件根节点
 */
class DevInsFileNode(
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "File"
    
    val frontMatter: DevInsFrontMatterNode?
        get() = findChild<DevInsFrontMatterNode>()
    
    val statements: List<DevInsStatementNode>
        get() = findChildren<DevInsStatementNode>()
    
    val codeBlocks: List<DevInsCodeBlockNode>
        get() = findChildren<DevInsCodeBlockNode>()
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitFile(this)
    }
}

/**
 * 前置元数据节点
 */
class DevInsFrontMatterNode(
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "FrontMatter"
    
    val entries: List<DevInsFrontMatterEntryNode>
        get() = findChildren<DevInsFrontMatterEntryNode>()
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitFrontMatter(this)
    }
}

/**
 * 前置元数据条目节点
 */
class DevInsFrontMatterEntryNode(
    val key: DevInsNode,
    val value: DevInsNode?,
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "FrontMatterEntry"
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitFrontMatterEntry(this)
    }
}

/**
 * 表达式节点基类
 */
abstract class DevInsExpressionNode(
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitExpression(this)
    }
}

/**
 * 语句节点基类
 */
abstract class DevInsStatementNode(
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitStatement(this)
    }
}

/**
 * 代码块节点
 */
class DevInsCodeBlockNode(
    val language: String?,
    val content: String,
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "CodeBlock"
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitCodeBlock(this)
    }
}

/**
 * Used 节点（@agent, /command, $variable）
 */
class DevInsUsedNode(
    val type: UsedType,
    val identifier: DevInsNode,
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "Used($type)"
    
    enum class UsedType {
        AGENT,      // @agent
        COMMAND,    // /command
        VARIABLE    // $variable
    }
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitUsed(this)
    }
}

/**
 * 变量节点
 */
class DevInsVariableNode(
    val name: String,
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "Variable"
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitVariable(this)
    }
}

/**
 * 命令节点
 */
class DevInsCommandNode(
    val name: String,
    val arguments: List<DevInsNode>,
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "Command"
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitCommand(this)
    }
}

/**
 * Agent 节点
 */
class DevInsAgentNode(
    val name: String,
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "Agent"
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitAgent(this)
    }
}

/**
 * 模式节点
 */
class DevInsPatternNode(
    val pattern: String,
    val action: DevInsNode?,
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "Pattern"
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitPattern(this)
    }
}

/**
 * 函数调用节点
 */
class DevInsFunctionCallNode(
    val functionName: String,
    val arguments: List<DevInsNode>,
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "FunctionCall"
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitFunctionCall(this)
    }
}

/**
 * 字面量节点
 */
class DevInsLiteralNode(
    val value: Any,
    val literalType: LiteralType,
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "Literal($literalType)"
    
    enum class LiteralType {
        STRING,
        NUMBER,
        BOOLEAN,
        DATE
    }
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitLiteral(this)
    }
}

/**
 * 标识符节点
 */
class DevInsIdentifierNode(
    val name: String,
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "Identifier"
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitIdentifier(this)
    }
}

/**
 * 二元表达式节点
 */
class DevInsBinaryExpressionNode(
    val left: DevInsExpressionNode,
    val operator: DevInsTokenNode,
    val right: DevInsExpressionNode,
    children: List<DevInsNode>
) : DevInsExpressionNode(children) {
    
    override val nodeType: String = "BinaryExpression"
}

/**
 * 一元表达式节点
 */
class DevInsUnaryExpressionNode(
    val operator: DevInsTokenNode,
    val operand: DevInsExpressionNode,
    children: List<DevInsNode>
) : DevInsExpressionNode(children) {
    
    override val nodeType: String = "UnaryExpression"
}

/**
 * 条件表达式节点
 */
class DevInsConditionalExpressionNode(
    val condition: DevInsExpressionNode,
    val thenBranch: DevInsNode,
    val elseBranch: DevInsNode?,
    children: List<DevInsNode>
) : DevInsExpressionNode(children) {
    
    override val nodeType: String = "ConditionalExpression"
}

/**
 * 生命周期节点
 */
class DevInsLifecycleNode(
    val lifecycleType: LifecycleType,
    val body: DevInsNode,
    children: List<DevInsNode>
) : DevInsStatementNode(children) {
    
    override val nodeType: String = "Lifecycle($lifecycleType)"
    
    enum class LifecycleType {
        WHEN,
        ON_STREAMING,
        BEFORE_STREAMING,
        ON_STREAMING_END,
        AFTER_STREAMING
    }
}

/**
 * 文本段节点
 */
class DevInsTextSegmentNode(
    private val textContent: String,
    children: List<DevInsNode>
) : DevInsCompositeNode(children) {

    override val nodeType: String = "TextSegment"

    override fun getText(): String = textContent

    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitDefault(this)
    }
}
