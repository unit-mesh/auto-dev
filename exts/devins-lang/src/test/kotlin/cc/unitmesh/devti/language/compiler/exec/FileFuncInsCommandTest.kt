package cc.unitmesh.devti.language.compiler.exec;

import org.junit.Assert.*
import org.junit.Test

class JavaAutoTestServiceTest {

    @Test
    fun shouldParseRegexCorrectly() {
        val prop = "By.tagName(\"a\")"
        val expectedResult = Pair("tagName", listOf("\"a\""))

        val result = parseRegex(prop)

        assertEquals(expectedResult, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowExceptionForInvalidPattern() {
        val prop = "click"

        parseRegex(prop)
    }
}
