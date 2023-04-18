package cc.unitmesh.devti.prompt

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Node
import org.commonmark.parser.Parser

fun parseCodeFromString(markdown: String): String {
    val parser: Parser = Parser.builder().build()
    val node: Node = parser.parse(markdown)
    val visitor = CodeVisitor()
    node.accept(visitor)
    return visitor.code
}

internal class CodeVisitor : AbstractVisitor() {
    var code = ""

    override fun visit(fencedCodeBlock: FencedCodeBlock?) {
        this.code = fencedCodeBlock?.literal ?: ""
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock?) {
        super.visit(indentedCodeBlock)
    }
}