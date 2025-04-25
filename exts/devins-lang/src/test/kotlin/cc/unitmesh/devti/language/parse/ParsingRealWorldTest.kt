package cc.unitmesh.devti.language.parse

import cc.unitmesh.devti.language.parser.DevInParserDefinition
import com.intellij.testFramework.ParsingTestCase

class ParsingRealWorldTest : ParsingTestCase("realworld", "devin", DevInParserDefinition()) {
    override fun getTestDataPath(): String {
        return "src/test/testData"
    }

    fun testAutotest() {
        doTest(true)
    }

    fun testLifeCycle() {
        doTest(true)
    }

    fun testContentTee() {
        doTest(true)
    }

    fun testWhenAfterStreaming() {
        doTest(true)
    }

    fun testAfterStreamingOnly() {
        doTest(true)
    }

    fun testOutputInVariable() {
        doTest(true)
    }

    fun testOnPaste() {
        doTest(true)
    }

    fun testLoginCommit() {
        doTest(true)
    }
}

