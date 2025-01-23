package cc.unitmesh.devti.language

import cc.unitmesh.devti.language.parser.DevInParserDefinition
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

    fun testJavaAnnotation() {
        doTest(true)
    }

    fun testBlockStartOnly() {
        doTest(true)
    }

    fun testComplexLangId() {
        doTest(true)
    }

    fun testAutoCommand() {
        doTest(true)
    }

//    fun testCommandAndSymbol() {
//        doTest(true)
//    }

    fun testBrowseWeb() {
        doTest(true)
    }

    fun testAutoRefactor() {
        doTest(true)
    }
}
