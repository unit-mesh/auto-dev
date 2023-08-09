package cc.unitmesh.devti.language

import cc.unitmesh.devti.runconfig.DevtiAnnotator
import org.junit.Test

class DevtiAnnotatorTest {
    @Test
    fun testMatchByString() {
        val input = "// devti://story/github/1234"
        val result = DevtiAnnotator.matchByString(input)
        assert(result != null)
        assert(result?.storyId == 1234)
        assert(result?.storySource == "github")
    }
}