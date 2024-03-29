package cc.unitmesh.devti.language.agenttool.browse

import cc.unitmesh.devti.language.agenttool.AgentToolContext
import cc.unitmesh.devti.language.agenttool.AgentTool
import cc.unitmesh.devti.language.agenttool.AgentToolResult
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class BrowseTool : AgentTool {
    override val name: String get() = "Browse"
    override val description: String = "Get the content of a given URL."

    override fun execute(context: AgentToolContext): AgentToolResult {
        return AgentToolResult(
            isSuccess = true,
            output = parse(context.argument).body
        )
    }

    companion object {
        /**
         * Doc for parseHtml
         *
         * Intellij API: [com.intellij.inspectopedia.extractor.utils.HtmlUtils.cleanupHtml]
         */
        fun parse(url: String): DocumentContent {
            val doc: Document = Jsoup.connect(url).get()
            return DocumentCleaner().cleanHtml(doc)
        }
    }
}

