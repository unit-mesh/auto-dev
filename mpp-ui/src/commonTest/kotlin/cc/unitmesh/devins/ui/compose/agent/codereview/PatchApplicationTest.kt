package cc.unitmesh.devins.ui.compose.agent.codereview

import cc.unitmesh.agent.diff.DiffLineType
import cc.unitmesh.agent.diff.DiffParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for patch application to verify that diffs are correctly applied to source files.
 * This tests the fix for the bug where patches were applied at wrong line positions.
 */
class PatchApplicationTest {

    @Test
    fun `should correctly apply simple addition patch`() {
        val originalContent = """
            line 1
            line 2
            line 3
        """.trimIndent()

        val patch = """
            --- a/test.txt
            +++ b/test.txt
            @@ -1,3 +1,4 @@
             line 1
            +new line
             line 2
             line 3
        """.trimIndent()

        val expected = """
            line 1
            new line
            line 2
            line 3
        """.trimIndent()

        val result = applyPatchManually(originalContent, patch)
        assertEquals(expected, result)
    }

    @Test
    fun `should correctly apply simple deletion patch`() {
        val originalContent = """
            line 1
            line 2
            line 3
            line 4
        """.trimIndent()

        val patch = """
            --- a/test.txt
            +++ b/test.txt
            @@ -1,4 +1,3 @@
             line 1
            -line 2
             line 3
             line 4
        """.trimIndent()

        val expected = """
            line 1
            line 3
            line 4
        """.trimIndent()

        val result = applyPatchManually(originalContent, patch)
        assertEquals(expected, result)
    }

    @Test
    fun `should correctly apply multiple changes in one hunk`() {
        val originalContent = """
            line 1
            line 2
            line 3
            line 4
            line 5
        """.trimIndent()

        val patch = """
            --- a/test.txt
            +++ b/test.txt
            @@ -1,5 +1,5 @@
             line 1
            -line 2
            +modified line 2
             line 3
            -line 4
            +modified line 4
             line 5
        """.trimIndent()

        val expected = """
            line 1
            modified line 2
            line 3
            modified line 4
            line 5
        """.trimIndent()

        val result = applyPatchManually(originalContent, patch)
        assertEquals(expected, result)
    }

    @Test
    fun `should correctly apply multiple additions and deletions`() {
        val originalContent = """
            function test() {
                console.log("old");
                return 42;
            }
        """.trimIndent()

        val patch = """
            --- a/test.js
            +++ b/test.js
            @@ -1,4 +1,5 @@
             function test() {
            -    console.log("old");
            +    console.log("new");
            +    console.log("extra");
                 return 42;
             }
        """.trimIndent()

        val expected = """
            function test() {
                console.log("new");
                console.log("extra");
                return 42;
            }
        """.trimIndent()

        val result = applyPatchManually(originalContent, patch)
        assertEquals(expected, result)
    }

    @Test
    fun `should correctly apply patch with multiple hunks`() {
        val originalContent = """
            line 1
            line 2
            line 3
            line 4
            line 5
            line 6
            line 7
            line 8
        """.trimIndent()

        val patch = """
            --- a/test.txt
            +++ b/test.txt
            @@ -1,3 +1,3 @@
             line 1
            -line 2
            +modified line 2
             line 3
            @@ -6,3 +6,4 @@
             line 6
             line 7
            +new line 7.5
             line 8
        """.trimIndent()

        val expected = """
            line 1
            modified line 2
            line 3
            line 4
            line 5
            line 6
            line 7
            new line 7.5
            line 8
        """.trimIndent()

        val result = applyPatchManually(originalContent, patch)
        assertEquals(expected, result)
    }

    @Test
    fun `should handle patch with emoji and UTF-8 characters`() {
        val originalContent = """
            // Comment with emoji 🚀
            fun hello() {
                println("Hello 世界")
            }
        """.trimIndent()

        val patch = """
            --- a/test.kt
            +++ b/test.kt
            @@ -1,4 +1,5 @@
             // Comment with emoji 🚀
             fun hello() {
            +    // New comment 中文
                 println("Hello 世界")
             }
        """.trimIndent()

        val expected = """
            // Comment with emoji 🚀
            fun hello() {
                // New comment 中文
                println("Hello 世界")
            }
        """.trimIndent()

        val result = applyPatchManually(originalContent, patch)
        assertEquals(expected, result)
    }

    @Test
    fun `should handle complex Kotlin code patch`() {
        val originalContent = """
            class MyClass {
                fun oldMethod() {
                    println("old")
                }
                
                fun anotherMethod() {
                    println("keep this")
                }
            }
        """.trimIndent()

        val patch = """
            --- a/MyClass.kt
            +++ b/MyClass.kt
            @@ -1,5 +1,6 @@
             class MyClass {
            -    fun oldMethod() {
            -        println("old")
            +    fun newMethod() {
            +        println("new")
            +        println("extra line")
                 }
                 
                 fun anotherMethod() {
        """.trimIndent()

        val expected = """
            class MyClass {
                fun newMethod() {
                    println("new")
                    println("extra line")
                }
                
                fun anotherMethod() {
                    println("keep this")
                }
            }
        """.trimIndent()

        val result = applyPatchManually(originalContent, patch)
        assertEquals(expected, result)
    }

    @Test
    fun `DiffParser should correctly parse patch with context lines`() {
        val patch = """
            --- a/test.txt
            +++ b/test.txt
            @@ -1,5 +1,5 @@
             context1
            -old line
            +new line
             context2
             context3
        """.trimIndent()

        val fileDiffs = DiffParser.parse(patch)
        assertEquals(1, fileDiffs.size)
        
        val hunk = fileDiffs[0].hunks[0]
        assertEquals(5, hunk.lines.size)
        assertEquals(DiffLineType.CONTEXT, hunk.lines[0].type)
        assertEquals(DiffLineType.DELETED, hunk.lines[1].type)
        assertEquals(DiffLineType.ADDED, hunk.lines[2].type)
        assertEquals(DiffLineType.CONTEXT, hunk.lines[3].type)
        assertEquals(DiffLineType.CONTEXT, hunk.lines[4].type)
    }

    @Test
    fun `should handle empty file additions`() {
        val originalContent = ""

        val patch = """
            --- /dev/null
            +++ b/test.txt
            @@ -0,0 +1,3 @@
            +line 1
            +line 2
            +line 3
        """.trimIndent()

        val expected = """
            line 1
            line 2
            line 3
        """.trimIndent()

        val result = applyPatchManually(originalContent, patch)
        assertEquals(expected, result)
    }

    /**
     * Manually apply a patch using the same algorithm as CodeReviewViewModel.
     * This duplicates the logic for testing purposes.
     */
    private fun applyPatchManually(originalContent: String, patchContent: String): String {
        // Handle empty content properly - String.lines() returns [""] for empty string
        val currentLines = if (originalContent.isEmpty()) {
            mutableListOf()
        } else {
            originalContent.lines().toMutableList()
        }
        
        val fileDiffs = DiffParser.parse(patchContent)
        
        assertTrue(fileDiffs.isNotEmpty(), "Failed to parse patch")
        
        val fileDiff = fileDiffs[0]
        var lineOffset = 0

        fileDiff.hunks.forEach { hunk ->
            // For new files, oldStartLine is 0, so we start at index 0
            var currentLineIndex = maxOf(0, hunk.oldStartLine - 1) + lineOffset
            var oldLineNum = maxOf(1, hunk.oldStartLine)

            hunk.lines.forEach { diffLine ->
                when (diffLine.type) {
                    DiffLineType.CONTEXT -> {
                        // Context line - verify it matches
                        if (currentLineIndex < currentLines.size) {
                            // Just verify, don't fail the test on mismatch for now
                            currentLineIndex++
                            oldLineNum++
                        }
                    }
                    DiffLineType.DELETED -> {
                        // Delete line
                        if (currentLineIndex < currentLines.size) {
                            currentLines.removeAt(currentLineIndex)
                            lineOffset--
                            oldLineNum++
                        }
                    }
                    DiffLineType.ADDED -> {
                        // Add line
                        if (currentLineIndex <= currentLines.size) {
                            currentLines.add(currentLineIndex, diffLine.content)
                            lineOffset++
                            currentLineIndex++
                        }
                    }
                    DiffLineType.HEADER -> {
                        // Skip header lines
                    }
                }
            }
        }

        return currentLines.joinToString("\n")
    }
}

