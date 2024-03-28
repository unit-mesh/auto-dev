package cc.unitmesh.devti.language.agenttool.scrapy;

import cc.unitmesh.devti.language.agenttool.browse.BrowseTool
import org.junit.Test


class BrowseToolTest {
    @Test
    fun should_clean_html_with_simple_tags() {
        // When
        val cleanedHtml = BrowseTool.parse("https://ide.unitmesh.cc/")
        // Then
        println(cleanedHtml)
    }
}