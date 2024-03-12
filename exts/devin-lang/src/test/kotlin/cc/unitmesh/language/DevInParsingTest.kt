package cc.unitmesh.language

import com.intellij.testFramework.ParsingTestCase

class DevInParsingTest : ParsingTestCase("parser", "devin", DevInParserDefinition()) {
    override fun getTestDataPath(): String {
        return "src/test/testData"
    }

    fun testBasicTest() {
        doTest(true)
    }

    fun testJavaHelloWorld() {
        doTest(true)
    }

    fun testEmptyCodeFence() {
        doTest(true)
    }
}
