package cc.unitmesh.devti.agent.tool.browse

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class Browse {
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

