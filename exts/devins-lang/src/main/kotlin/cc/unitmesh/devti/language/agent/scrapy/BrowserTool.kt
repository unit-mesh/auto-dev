package cc.unitmesh.devti.language.agent.scrapy

import cc.unitmesh.devti.language.agent.AgentContext
import cc.unitmesh.devti.language.agent.AgentTool
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class BrowserTool: AgentTool {
    override fun execute(context: AgentContext) {
        TODO("Not yet implemented")
    }

    /**
     * Doc for parseHtml
     *
     * Intellij API: [com.intellij.inspectopedia.extractor.utils.HtmlUtils.cleanupHtml]
     */
    fun parseHtml(url: String) {
        val doc: Document = Jsoup.connect(url).get()
        val html = doc.body()
//        Jsoup.parse(html)
    }
}

