package cc.unitmesh.devti.language.agenttool.browse

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class DocumentCleaner {
    fun cleanHtml(html: String): DocumentContent {
        val doc = Jsoup.parse(html)
        return cleanHtml(doc)
    }

    fun cleanHtml(doc: Document): DocumentContent {
        return DocumentContent(
            title = doc.title(),
            language = metaContent(doc, "http-equiv", "Content-Language"),
            description = metaDescription(doc),
            body = articleNode(doc)
        )
    }

    fun metaDescription(doc: Document): String? {
        val attributes = arrayOf(arrayOf("property", "description"), arrayOf("name", "description"))
        return attributes
            .asSequence()
            .mapNotNull { (key, value) -> metaContent(doc, key, value) }
            .firstOrNull()
    }

    fun metaContent(doc: Document, key: String, value: String): String? {
        val metaElements = doc.select("head meta[$key=$value]")
        return metaElements
            .map { it.attr("content").trim() }
            .firstOrNull { it.isNotEmpty() }
    }

    val ARTICLE_BODY_ATTR: Array<Pair<String, String>> = arrayOf(
        Pair("itemprop", "articleBody"),
        Pair("data-testid", "article-body"),
        Pair("name", "articleBody")
    )

    fun articleNode(doc: Document): String? {
        var bodyElement: Element? = doc.select("html").select("body").first()
        val firstBodyElement = bodyElement ?: return null
        // the Microdata
        for ((attr, value) in ARTICLE_BODY_ATTR) {
            bodyElement = doc.selectFirst("[$attr=$value]")
            if (bodyElement != null) {
                return bodyElement.text()
            }
        }

        return trySelectBestCode(firstBodyElement)
    }

    private fun trySelectBestCode(doc: Element): String {
        val commonBestNodes = doc.select("article, main, #main, #content, #doc-content, #contents, .book-body")
        if (commonBestNodes.isNotEmpty()) {
            return commonBestNodes.first()?.text() ?: ""
        }

        return doc.text()
    }
}