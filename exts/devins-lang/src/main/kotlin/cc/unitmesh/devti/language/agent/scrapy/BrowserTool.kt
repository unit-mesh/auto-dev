package cc.unitmesh.devti.language.agent.scrapy

import cc.unitmesh.devti.language.agent.AgentContext
import cc.unitmesh.devti.language.agent.AgentTool
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import java.util.function.Consumer

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
        fun cleanHtml(src: String): String {
            val doc = Jsoup.parse(Jsoup.clean(src, WHITELIST))

            doc.select("ul").forEach(Consumer { e: Element ->
                e.tagName("list")
            })

            doc.select("ol").forEach(Consumer { e: Element ->
                e.tagName("list")
                e.attr("type", "decimal")
            })

            doc.select("code").forEach(Consumer { element: Element ->
                element.text(
                    element.text()
                )
            })

            doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
            return doc.body().html()
        }
    }
}

