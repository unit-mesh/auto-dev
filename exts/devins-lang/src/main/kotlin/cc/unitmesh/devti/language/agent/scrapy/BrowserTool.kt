package cc.unitmesh.devti.language.agent.scrapy

import cc.unitmesh.devti.language.agent.AgentContext
import cc.unitmesh.devti.language.agent.AgentTool
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist

class BrowserTool : AgentTool {
    override fun execute(context: AgentContext) {
        TODO("Not yet implemented")
    }

    companion object {
        private val WHITELIST: Safelist = Safelist()
            .addTags("p", "br", "li", "ul", "ol", "b", "i", "code", "a")
            .addAttributes("a", "href")

        /**
         * Doc for parseHtml
         *
         * Intellij API: [com.intellij.inspectopedia.extractor.utils.HtmlUtils.cleanupHtml]
         */
        fun cleanHtml(url: String): DocumentContent {
            val doc: Document = Jsoup.connect(url).get()
            return DocumentCleaner().cleanHtml(doc)
        }
    }
}

