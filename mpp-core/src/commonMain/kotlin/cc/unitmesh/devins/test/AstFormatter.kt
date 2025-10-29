package cc.unitmesh.devins.test

import cc.unitmesh.devins.ast.*

/**
 * AST 格式化器
 * 将 AST 格式化为类似于 IDEA 平台测试的字符串表示
 */
class AstFormatter {
    
    private val builder = StringBuilder()
    private var indentLevel = 0
    
    /**
     * 格式化 AST 节点
     */
    fun format(node: DevInsNode): String {
        builder.clear()
        indentLevel = 0
        formatNode(node)
        return builder.toString()
    }
    
    /**
     * 格式化单个节点
     */
    private fun formatNode(node: DevInsNode) {
        when (node) {
            is DevInsFileNode -> formatFileNode(node)
            is DevInsFrontMatterNode -> formatFrontMatterNode(node)
            is DevInsFrontMatterEntryNode -> formatFrontMatterEntryNode(node)
            is DevInsCodeBlockNode -> formatCodeBlockNode(node)
            is DevInsUsedNode -> formatUsedNode(node)
            is DevInsVariableNode -> formatVariableNode(node)
            is DevInsCommandNode -> formatCommandNode(node)
            is DevInsAgentNode -> formatAgentNode(node)
            is DevInsPatternNode -> formatPatternNode(node)
            is DevInsFunctionCallNode -> formatFunctionCallNode(node)
            is DevInsLiteralNode -> formatLiteralNode(node)
            is DevInsIdentifierNode -> formatIdentifierNode(node)
            is DevInsBinaryExpressionNode -> formatBinaryExpressionNode(node)
            is DevInsUnaryExpressionNode -> formatUnaryExpressionNode(node)
            is DevInsConditionalExpressionNode -> formatConditionalExpressionNode(node)
            is DevInsLifecycleNode -> formatLifecycleNode(node)
            is DevInsTextSegmentNode -> formatTextSegmentNode(node)
            is DevInsTokenNode -> formatTokenNode(node)
            is DevInsErrorNode -> formatErrorNode(node)
            else -> formatGenericNode(node)
        }
    }
    
    private fun formatFileNode(node: DevInsFileNode) {
        appendLine("DevInFile")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatFrontMatterNode(node: DevInsFrontMatterNode) {
        appendLine("DevInFrontMatterHeaderImpl(FRONT_MATTER_HEADER)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatFrontMatterEntryNode(node: DevInsFrontMatterEntryNode) {
        appendLine("DevInFrontMatterEntryImpl(FRONT_MATTER_ENTRY)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatCodeBlockNode(node: DevInsCodeBlockNode) {
        appendLine("CodeBlockElement(CODE)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatUsedNode(node: DevInsUsedNode) {
        val typeName = when (node.type) {
            DevInsUsedNode.UsedType.AGENT -> "AGENT"
            DevInsUsedNode.UsedType.COMMAND -> "COMMAND"
            DevInsUsedNode.UsedType.VARIABLE -> "VARIABLE"
        }
        appendLine("DevInUsedImpl(USED)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatVariableNode(node: DevInsVariableNode) {
        appendLine("DevInVariableImpl(VARIABLE)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatCommandNode(node: DevInsCommandNode) {
        appendLine("DevInCommandImpl(COMMAND)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatAgentNode(node: DevInsAgentNode) {
        appendLine("DevInAgentImpl(AGENT)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatPatternNode(node: DevInsPatternNode) {
        appendLine("PatternElement(PATTERN)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatFunctionCallNode(node: DevInsFunctionCallNode) {
        appendLine("DevInFuncCallImpl(FUNC_CALL)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatLiteralNode(node: DevInsLiteralNode) {
        val typeName = when (node.literalType) {
            DevInsLiteralNode.LiteralType.STRING -> "LITERAL_EXPR"
            DevInsLiteralNode.LiteralType.NUMBER -> "LITERAL_EXPR"
            DevInsLiteralNode.LiteralType.BOOLEAN -> "LITERAL_EXPR"
            DevInsLiteralNode.LiteralType.DATE -> "LITERAL_EXPR"
        }
        appendLine("DevInLiteralExprImpl($typeName)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatIdentifierNode(node: DevInsIdentifierNode) {
        appendLine("DevInIdentifierImpl(IDENTIFIER)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatBinaryExpressionNode(node: DevInsBinaryExpressionNode) {
        appendLine("DevInBinaryExprImpl(BINARY_EXPR)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatUnaryExpressionNode(node: DevInsUnaryExpressionNode) {
        appendLine("DevInUnaryExprImpl(UNARY_EXPR)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatConditionalExpressionNode(node: DevInsConditionalExpressionNode) {
        appendLine("DevInConditionalExprImpl(CONDITIONAL_EXPR)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatLifecycleNode(node: DevInsLifecycleNode) {
        val typeName = when (node.lifecycleType) {
            DevInsLifecycleNode.LifecycleType.WHEN -> "WHEN"
            DevInsLifecycleNode.LifecycleType.ON_STREAMING -> "ON_STREAMING"
            DevInsLifecycleNode.LifecycleType.BEFORE_STREAMING -> "BEFORE_STREAMING"
            DevInsLifecycleNode.LifecycleType.ON_STREAMING_END -> "ON_STREAMING_END"
            DevInsLifecycleNode.LifecycleType.AFTER_STREAMING -> "AFTER_STREAMING"
        }
        appendLine("DevInLifecycleImpl($typeName)")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatTextSegmentNode(node: DevInsTextSegmentNode) {
        appendLine("PsiElement(DevInTokenType.TEXT_SEGMENT)('${escapeString(node.getText())}')")
    }
    
    private fun formatTokenNode(node: DevInsTokenNode) {
        val tokenType = node.token.type
        val text = escapeString(node.token.text)
        appendLine("PsiElement(DevInTokenType.$tokenType)('$text')")
    }
    
    private fun formatErrorNode(node: DevInsErrorNode) {
        appendLine("PsiErrorElement:${node.message}")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun formatGenericNode(node: DevInsNode) {
        appendLine("${node.nodeType}Impl(${node.nodeType.uppercase()})")
        indentLevel++
        node.children.forEach { formatNode(it) }
        indentLevel--
    }
    
    private fun appendLine(text: String) {
        repeat(indentLevel) { builder.append("  ") }
        builder.appendLine(text)
    }
    
    private fun escapeString(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
