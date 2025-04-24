package cc.unitmesh.devti.language.compiler.execute

import cc.unitmesh.devti.language.processor.CrawlProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CrawlProcessorTest : BasePlatformTestCase() {
    fun testShouldParseLink() {
        val urls = arrayOf("https://shire.phodal.com/")
        val results = CrawlProcessor.execute(urls)
        assertEquals(results.size, 1)
    }
}
