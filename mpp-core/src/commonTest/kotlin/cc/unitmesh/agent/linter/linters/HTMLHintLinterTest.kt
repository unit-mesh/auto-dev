package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HTMLHintLinterTest {
    @Test
    fun `should parse htmlhint output correctly`() {
        val output = """
   /path/to/bad.html
      L10 |        <p>This paragraph is not closed
                   ^ Tag must be paired, missing: [ </p> ], start tag match failed [ <p> ] on line 10. (tag-pair)
      L15 |    <div id="test">Duplicate ID</div>
                   ^ The id value [ test ] must be unique. (id-unique)
      L18 |    <div style="color: red;">Inline style is bad</div>
                   ^ Inline style cannot be used. (inline-style-disabled)

Scanned 1 files, found 3 errors in 1 files (66 ms)
        """.trimIndent()

        val filePath = "bad.html"
        val issues = HTMLHintLinter.parseHTMLHintOutput(output, filePath)

        assertEquals(3, issues.size, "Should parse 3 issues")

        // Check tag-pair error
        val tagPairIssue = issues[0]
        assertEquals(10, tagPairIssue.line)
        assertEquals("tag-pair", tagPairIssue.rule)
        assertEquals(cc.unitmesh.linter.LintSeverity.ERROR, tagPairIssue.severity)
        assertTrue(tagPairIssue.message.contains("Tag must be paired"))

        // Check id-unique error
        val idUniqueIssue = issues[1]
        assertEquals(15, idUniqueIssue.line)
        assertEquals("id-unique", idUniqueIssue.rule)
        assertTrue(idUniqueIssue.message.contains("must be unique"))

        // Check inline-style error
        val styleIssue = issues[2]
        assertEquals(18, styleIssue.line)
        assertEquals("inline-style-disabled", styleIssue.rule)
        assertTrue(styleIssue.message.contains("Inline style"))
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = HTMLHintLinter.parseHTMLHintOutput(output, "test.html")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle clean html`() {
        val output = """
test.html

Scanned 1 files, found 0 errors in 0 files (10 ms)
        """.trimIndent()
        val issues = HTMLHintLinter.parseHTMLHintOutput(output, "test.html")
        assertEquals(0, issues.size)
    }
}

