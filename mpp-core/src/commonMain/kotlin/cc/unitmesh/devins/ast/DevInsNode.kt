package cc.unitmesh.devins.ast

import cc.unitmesh.devins.token.DevInsToken
import cc.unitmesh.devins.token.TokenPosition

/**
 * DevIns AST 节点基类
 * 所有 AST 节点都继承自这个基类
 */
abstract class DevInsNode {
    /**
     * 节点在源代码中的起始位置
     */
    abstract val startPosition: TokenPosition
    
    /**
     * 节点在源代码中的结束位置
     */
    abstract val endPosition: TokenPosition
    
    /**
     * 节点的子节点列表
     */
    abstract val children: List<DevInsNode>
    
    /**
     * 节点类型名称
     */
    abstract val nodeType: String
    
    /**
     * 接受访问者模式
     */
    abstract fun <T> accept(visitor: DevInsVisitor<T>): T
    
    /**
     * 获取节点的文本表示
     */
    open fun getText(): String {
        return children.joinToString("") { it.getText() }
    }
    
    /**
     * 查找指定类型的子节点
     */
    inline fun <reified T : DevInsNode> findChild(): T? {
        return children.firstOrNull { it is T } as? T
    }
    
    /**
     * 查找所有指定类型的子节点
     */
    inline fun <reified T : DevInsNode> findChildren(): List<T> {
        return children.filterIsInstance<T>()
    }
    
    /**
     * 递归查找指定类型的所有后代节点
     */
    fun findDescendantsByType(nodeType: String): List<DevInsNode> {
        val result = mutableListOf<DevInsNode>()

        fun collectDescendants(node: DevInsNode) {
            if (node.nodeType == nodeType) {
                result.add(node)
            }
            node.children.forEach { collectDescendants(it) }
        }

        collectDescendants(this)
        return result
    }
    
    override fun toString(): String {
        return "$nodeType(${startPosition.line}:${startPosition.column})"
    }
}

/**
 * 叶子节点基类
 * 表示没有子节点的 AST 节点
 */
abstract class DevInsLeafNode : DevInsNode() {
    override val children: List<DevInsNode> = emptyList()
}

/**
 * Token 节点
 * 表示单个 Token 的 AST 节点
 */
class DevInsTokenNode(
    val token: DevInsToken
) : DevInsLeafNode() {
    
    override val startPosition: TokenPosition
        get() = TokenPosition(token.startOffset, token.line, token.column)
    
    override val endPosition: TokenPosition
        get() = TokenPosition(token.endOffset, token.line, token.column + token.length)
    
    override val nodeType: String = "Token(${token.type})"
    
    override fun getText(): String = token.text
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitToken(this)
    }
    
    override fun toString(): String {
        return "TokenNode(${token.type}, '${token.text}', ${startPosition})"
    }
}

/**
 * 复合节点基类
 * 表示有子节点的 AST 节点
 */
abstract class DevInsCompositeNode(
    override val children: List<DevInsNode>
) : DevInsNode() {
    
    override val startPosition: TokenPosition
        get() = children.firstOrNull()?.startPosition ?: TokenPosition(0, 1, 1)
    
    override val endPosition: TokenPosition
        get() = children.lastOrNull()?.endPosition ?: TokenPosition(0, 1, 1)
}

/**
 * 错误节点
 * 表示解析过程中遇到的错误
 */
class DevInsErrorNode(
    val message: String,
    val position: TokenPosition,
    children: List<DevInsNode> = emptyList()
) : DevInsCompositeNode(children) {
    
    override val nodeType: String = "Error"
    
    override val startPosition: TokenPosition = position
    override val endPosition: TokenPosition = position
    
    override fun <T> accept(visitor: DevInsVisitor<T>): T {
        return visitor.visitError(this)
    }
    
    override fun toString(): String {
        return "ErrorNode('$message', $position)"
    }
}

/**
 * 访问者接口
 * 用于遍历和处理 AST 节点
 */
interface DevInsVisitor<T> {
    fun visitToken(node: DevInsTokenNode): T
    fun visitError(node: DevInsErrorNode): T
    fun visitFile(node: DevInsFileNode): T
    fun visitFrontMatter(node: DevInsFrontMatterNode): T
    fun visitFrontMatterEntry(node: DevInsFrontMatterEntryNode): T
    fun visitExpression(node: DevInsExpressionNode): T
    fun visitStatement(node: DevInsStatementNode): T
    fun visitCodeBlock(node: DevInsCodeBlockNode): T
    fun visitUsed(node: DevInsUsedNode): T
    fun visitVariable(node: DevInsVariableNode): T
    fun visitCommand(node: DevInsCommandNode): T
    fun visitAgent(node: DevInsAgentNode): T
    fun visitPattern(node: DevInsPatternNode): T
    fun visitFunctionCall(node: DevInsFunctionCallNode): T
    fun visitLiteral(node: DevInsLiteralNode): T
    fun visitIdentifier(node: DevInsIdentifierNode): T
    
    /**
     * 默认访问方法
     */
    fun visitDefault(node: DevInsNode): T
}

/**
 * 基础访问者实现
 * 提供默认的访问行为
 */
abstract class DevInsBaseVisitor<T> : DevInsVisitor<T> {
    
    protected abstract fun defaultResult(): T
    
    override fun visitToken(node: DevInsTokenNode): T = defaultResult()
    override fun visitError(node: DevInsErrorNode): T = defaultResult()
    override fun visitFile(node: DevInsFileNode): T = visitDefault(node)
    override fun visitFrontMatter(node: DevInsFrontMatterNode): T = visitDefault(node)
    override fun visitFrontMatterEntry(node: DevInsFrontMatterEntryNode): T = visitDefault(node)
    override fun visitExpression(node: DevInsExpressionNode): T = visitDefault(node)
    override fun visitStatement(node: DevInsStatementNode): T = visitDefault(node)
    override fun visitCodeBlock(node: DevInsCodeBlockNode): T = visitDefault(node)
    override fun visitUsed(node: DevInsUsedNode): T = visitDefault(node)
    override fun visitVariable(node: DevInsVariableNode): T = visitDefault(node)
    override fun visitCommand(node: DevInsCommandNode): T = visitDefault(node)
    override fun visitAgent(node: DevInsAgentNode): T = visitDefault(node)
    override fun visitPattern(node: DevInsPatternNode): T = visitDefault(node)
    override fun visitFunctionCall(node: DevInsFunctionCallNode): T = visitDefault(node)
    override fun visitLiteral(node: DevInsLiteralNode): T = visitDefault(node)
    override fun visitIdentifier(node: DevInsIdentifierNode): T = visitDefault(node)
    
    override fun visitDefault(node: DevInsNode): T {
        return defaultResult()
    }
}


