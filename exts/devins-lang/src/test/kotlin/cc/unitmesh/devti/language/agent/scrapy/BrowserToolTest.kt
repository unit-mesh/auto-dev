package cc.unitmesh.devti.language.agent.scrapy;

import org.junit.Test


class BrowserToolTest {
    @Test
    fun should_clean_html_with_simple_tags() {
        // When
        val cleanedHtml = BrowserTool.cleanHtml("https://ide.unitmesh.cc/")
        // Then
        println(cleanedHtml)
    }
}