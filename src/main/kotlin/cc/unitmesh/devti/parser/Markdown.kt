package cc.unitmesh.devti.parser

import org.commonmark.Extension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

fun parseMarkdown(markdown: String): String {
    val extensions: List<Extension> = listOf(TablesExtension.create())
    val parser = Parser.builder()
        .extensions(extensions)
        .build()

    val document: Node = parser.parse(markdown)
    val htmlRenderer = HtmlRenderer
        .builder()
        .extensions(extensions)
        .build()
    return htmlRenderer.render(document)
}

fun parseCodeFromString(markdown: String): List<String> {
    val extensions: List<Extension> = listOf(TablesExtension.create())
    val parser: Parser = Parser.builder()
        .extensions(extensions)
        .build()

    val node: Node = parser.parse(markdown)
    val visitor = CodeVisitor()
    node.accept(visitor)

    if (visitor.code.isEmpty()) {
        // TODO: we need to add multiple code blocks support
        val isJavaMethod = markdown.contains("public ") || markdown.contains("private ") || markdown.contains("protected ")
        if (isJavaMethod) {
            return listOf(markdown)
        }

        return listOf(markdown)
    }

    return visitor.code
}

internal class CodeVisitor : AbstractVisitor() {
    var code = listOf<String>()

    override fun visit(fencedCodeBlock: FencedCodeBlock?) {
        if (fencedCodeBlock?.literal != null) {
            this.code += fencedCodeBlock.literal
        }
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock?) {
        super.visit(indentedCodeBlock)
    }
}