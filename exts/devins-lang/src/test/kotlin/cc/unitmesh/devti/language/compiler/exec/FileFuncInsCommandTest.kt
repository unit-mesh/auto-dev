package cc.unitmesh.devti.language.compiler.exec;

import org.junit.Assert.*
import org.junit.Test

class FileFuncInsCommandTest {

    @Test
    fun shouldParseRegexCorrectly() {
        val prop = "By.tagName(\"a\")"
        val expectedResult = Pair("tagName", listOf("\"a\""))

        val result = FileFuncInsCommand.parseRegex(prop)

        assertEquals(expectedResult, result)
    }

    @Test
    fun shouldReturnNullWhenError() {
        val prop = "click"

        val result = FileFuncInsCommand.parseRegex(prop)
        assertNull(result)
    }
}
