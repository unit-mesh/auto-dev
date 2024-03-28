package cc.unitmesh.devti.language.agent.scrapy

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class DocumentCleaner {
    fun cleanHtml(html: String): DocumentContent {
        val doc = Jsoup.parse(html)
        return DocumentContent(
            title = doc.title(),
            language = metaContent(doc, "http-equiv", "Content-Language"),
            description = metaDescription(doc),
            text = articleNode(doc)
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
        for ((attr, value) in ARTICLE_BODY_ATTR) {
            bodyElement = doc.selectFirst("[$attr=$value]")
            if (bodyElement != null) {
                // 如果找到了匹配的元素，则跳出循环
                break
            }
        }

        // 提取文章主体文本内容
        return bodyElement?.text() ?: firstBodyElement.text()
    }

}