package cc.unitmesh.devti.language.compiler.data

import cc.unitmesh.devti.language.compiler.model.LineInfo
import junit.framework.TestCase.assertEquals
import org.junit.Test

class LineInfoTest {

    @Test
    fun should_createLineInfo_when_validStringGiven() {
        // given
        val validString = "filepath#L1-L12"

        // when
        val result = LineInfo.fromString(validString)

        // then
        val expected = LineInfo(1, 12)
        assertEquals(expected, result)
    }

    @Test
    fun should_returnNull_when_invalidStringGiven() {
        // given
        val invalidString = "wrongStringFormat"

        // when
        val result = LineInfo.fromString(invalidString)

        // then
        assertEquals(null, result)
    }

    @Test
    fun should_returnNull_when_invalidStartLineGiven() {
        // given
        val invalidString = "filepath#Lxyz-L12"

        // when
        val result = LineInfo.fromString(invalidString)

        // then
        assertEquals(null, result)
    }

    @Test
    fun should_returnNull_when_invalidEndLineGiven() {
        // given
        val invalidString = "filepath#L1-Lxyz"

        // when
        val result = LineInfo.fromString(invalidString)

        // then
        assertEquals(null, result)
    }
}