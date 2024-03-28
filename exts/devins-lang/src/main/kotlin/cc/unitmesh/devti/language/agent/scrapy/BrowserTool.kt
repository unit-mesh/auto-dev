package cc.unitmesh.devti.language.agent.scrapy

import cc.unitmesh.devti.language.agent.AgentContext
import cc.unitmesh.devti.language.agent.AgentTool
import cc.unitmesh.devti.language.agent.ToolResult
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class BrowserTool : AgentTool {
    override fun execute(context: AgentContext): ToolResult {
        return ToolResult(
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

