package cc.unitmesh.agent.vcs.context

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChangedCodeExtractorTest {

    @Test
    fun `should extract changed hunks from simple git diff`() {
        val patch = """
            diff --git a/src/User.kt b/src/User.kt
            index abc1234..def5678 100644
            --- a/src/User.kt
            +++ b/src/User.kt
            @@ -13,7 +13,10 @@ class UserService {
                 fun processUser(user: User?) {
            -        println(user.name)
            +        if (user == null) {
            +            throw IllegalArgumentException("User cannot be null")
            +        }
            +        println(user.name)
                 }
             }
        """.trimIndent()

        val extractor = ChangedCodeExtractor()
        val result = extractor.extractChangedHunks(patch)

        assertEquals(1, result.size, "Should extract 1 file")
        assertTrue(result.containsKey("src/User.kt"), "Should contain User.kt")
        
        val hunks = result["src/User.kt"]!!
        assertEquals(1, hunks.size, "Should have 1 hunk")
        
        val hunk = hunks[0]
        assertEquals(13, hunk.oldStartLine)
        assertEquals(13, hunk.newStartLine)
        assertEquals(1, hunk.deletedLines.size, "Should have 1 deleted line")
        assertEquals(4, hunk.addedLines.size, "Should have 4 added lines")
        
        // Check deleted line content
        assertTrue(hunk.deletedLines[0].contains("println(user.name)"))
        
        // Check added lines content
        assertTrue(hunk.addedLines[0].contains("if (user == null)"))
        assertTrue(hunk.addedLines[1].contains("throw IllegalArgumentException"))
        assertTrue(hunk.addedLines[2].contains("}"))
        assertTrue(hunk.addedLines[3].contains("println(user.name)"))
    }

    @Test
    fun `should extract multiple hunks from same file`() {
        val patch = """
            diff --git a/src/Service.kt b/src/Service.kt
            index abc1234..def5678 100644
            --- a/src/Service.kt
            +++ b/src/Service.kt
            @@ -10,7 +10,7 @@ class Service {
                 fun method1() {
            -        val x = 5
            +        val x = 10
                 }
            @@ -50,6 +50,7 @@ class Service {
                 fun method2() {
                     val stream = openStream()
            +        stream.close()
                 }
            }
        """.trimIndent()

        val extractor = ChangedCodeExtractor()
        val result = extractor.extractChangedHunks(patch)

        assertEquals(1, result.size)
        val hunks = result["src/Service.kt"]!!
        assertEquals(2, hunks.size, "Should have 2 hunks")
        
        // First hunk
        assertEquals(10, hunks[0].oldStartLine)
        assertEquals(1, hunks[0].deletedLines.size)
        assertEquals(1, hunks[0].addedLines.size)
        
        // Second hunk
        assertEquals(50, hunks[1].oldStartLine)
        assertEquals(0, hunks[1].deletedLines.size)
        assertEquals(1, hunks[1].addedLines.size)
    }

    @Test
    fun `should extract hunks from multiple files`() {
        val patch = """
            diff --git a/src/FileA.kt b/src/FileA.kt
            index abc1234..def5678 100644
            --- a/src/FileA.kt
            +++ b/src/FileA.kt
            @@ -5,6 +5,7 @@ class FileA {
                 fun foo() {
            +        println("added line")
                 }
             }
             
            diff --git a/src/FileB.kt b/src/FileB.kt
            index abc1234..def5678 100644
            --- a/src/FileB.kt
            +++ b/src/FileB.kt
            @@ -10,7 +10,7 @@ class FileB {
                 fun bar() {
            -        val x = 1
            +        val x = 2
                 }
             }
        """.trimIndent()

        val extractor = ChangedCodeExtractor()
        val result = extractor.extractChangedHunks(patch)

        assertEquals(2, result.size, "Should extract 2 files")
        assertTrue(result.containsKey("src/FileA.kt"))
        assertTrue(result.containsKey("src/FileB.kt"))
        
        assertEquals(1, result["src/FileA.kt"]!!.size)
        assertEquals(1, result["src/FileB.kt"]!!.size)
    }

    @Test
    fun `should include context lines before and after changes`() {
        val patch = """
            diff --git a/src/Test.kt b/src/Test.kt
            index abc1234..def5678 100644
            --- a/src/Test.kt
            +++ b/src/Test.kt
            @@ -8,13 +8,14 @@ class Test {
                 line1
                 line2
                 line3
            -    oldLine
            +    newLine
                 line4
                 line5
                 line6
             }
        """.trimIndent()

        val extractor = ChangedCodeExtractor()
        val result = extractor.extractChangedHunks(patch, contextLines = 3)

        val hunk = result["src/Test.kt"]!![0]
        
        // Should have 3 context lines before
        assertEquals(3, hunk.contextBefore.size)
        assertTrue(hunk.contextBefore[0].contains("line1"))
        assertTrue(hunk.contextBefore[1].contains("line2"))
        assertTrue(hunk.contextBefore[2].contains("line3"))
        
        // Should have 3 context lines after
        assertEquals(3, hunk.contextAfter.size)
        assertTrue(hunk.contextAfter[0].contains("line4"))
        assertTrue(hunk.contextAfter[1].contains("line5"))
        assertTrue(hunk.contextAfter[2].contains("line6"))
        
        // Should have the change
        assertEquals(1, hunk.deletedLines.size)
        assertEquals(1, hunk.addedLines.size)
        assertTrue(hunk.deletedLines[0].contains("oldLine"))
        assertTrue(hunk.addedLines[0].contains("newLine"))
    }

    @Test
    fun `should respect contextLines parameter`() {
        val patch = """
            diff --git a/src/Test.kt b/src/Test.kt
            index abc1234..def5678 100644
            --- a/src/Test.kt
            +++ b/src/Test.kt
            @@ -5,9 +5,9 @@ class Test {
                 context1
                 context2
                 context3
            -    old
            +    new
                 context4
                 context5
             }
        """.trimIndent()

        val extractor = ChangedCodeExtractor()
        
        // Test with 1 context line
        val result1 = extractor.extractChangedHunks(patch, contextLines = 1)
        val hunk1 = result1["src/Test.kt"]!![0]
        assertEquals(1, hunk1.contextBefore.size)
        assertEquals(1, hunk1.contextAfter.size)
        
        // Test with 2 context lines
        val result2 = extractor.extractChangedHunks(patch, contextLines = 2)
        val hunk2 = result2["src/Test.kt"]!![0]
        assertEquals(2, hunk2.contextBefore.size)
        assertEquals(2, hunk2.contextAfter.size)
    }

    @Test
    fun `should handle empty patch`() {
        val extractor = ChangedCodeExtractor()
        val result = extractor.extractChangedHunks("")

        assertEquals(0, result.size, "Empty patch should return empty map")
    }

    @Test
    fun `should skip binary files`() {
        val patch = """
            diff --git a/image.png b/image.png
            index abc1234..def5678 100644
            Binary files a/image.png and b/image.png differ
        """.trimIndent()

        val extractor = ChangedCodeExtractor()
        val result = extractor.extractChangedHunks(patch)

        assertEquals(0, result.size, "Binary files should be skipped")
    }

    @Test
    fun `should handle new file creation`() {
        val patch = """
            diff --git a/newfile.kt b/newfile.kt
            new file mode 100644
            index 0000000..abc1234
            --- /dev/null
            +++ b/newfile.kt
            @@ -0,0 +1,5 @@
            +class NewFile {
            +    fun foo() {
            +        println("new")
            +    }
            +}
        """.trimIndent()

        val extractor = ChangedCodeExtractor()
        val result = extractor.extractChangedHunks(patch)

        assertEquals(1, result.size)
        assertTrue(result.containsKey("newfile.kt"))
        
        val hunk = result["newfile.kt"]!![0]
        assertEquals(5, hunk.addedLines.size, "Should have 5 added lines")
        assertEquals(0, hunk.deletedLines.size, "Should have no deleted lines")
    }

    @Test
    fun `should handle file deletion`() {
        val patch = """
            diff --git a/oldfile.kt b/oldfile.kt
            deleted file mode 100644
            index abc1234..0000000
            --- a/oldfile.kt
            +++ /dev/null
            @@ -1,3 +0,0 @@
            -class OldFile {
            -    // deleted
            -}
        """.trimIndent()

        val extractor = ChangedCodeExtractor()
        val result = extractor.extractChangedHunks(patch)

        // Deleted files might still be included with deletions
        // The behavior depends on implementation
        // For now, we just check it doesn't crash
        assertTrue(result.isEmpty() || result.size == 1)
    }

    @Test
    fun `should format hunks summary correctly`() {
        val patch = """
            diff --git a/src/File1.kt b/src/File1.kt
            --- a/src/File1.kt
            +++ b/src/File1.kt
            @@ -10,4 +10,5 @@ class File1 {
                 line1
            +    added
             }
            
            diff --git a/src/File2.kt b/src/File2.kt
            --- a/src/File2.kt
            +++ b/src/File2.kt
            @@ -5,5 +5,4 @@ class File2 {
            -    removed
                 line2
             }
        """.trimIndent()

        val extractor = ChangedCodeExtractor()
        val hunks = extractor.extractChangedHunks(patch)
        val summary = extractor.formatHunksSummary(hunks)

        assertTrue(summary.contains("File: src/File1.kt"))
        assertTrue(summary.contains("File: src/File2.kt"))
        assertTrue(summary.contains("Total: 2 files"))
    }

    @Test
    fun `toUnifiedDiff should format hunk correctly`() {
        val hunk = CodeHunk(
            oldStartLine = 10,
            newStartLine = 10,
            oldLineCount = 3,
            newLineCount = 4,
            contextBefore = listOf("    context1"),
            addedLines = listOf("    added1", "    added2"),
            deletedLines = listOf("    old1"),
            contextAfter = listOf("    context2"),
            header = "@@ -10,3 +10,4 @@ class Test {"
        )

        val formatted = hunk.toUnifiedDiff()

        assertTrue(formatted.contains("@@ -10,3 +10,4 @@ class Test {"))
        assertTrue(formatted.contains("     context1")) // Space prefix + 4 spaces from content
        assertTrue(formatted.contains("-    old1")) // Minus prefix + 4 spaces from content
        assertTrue(formatted.contains("+    added1")) // Plus prefix + 4 spaces from content
        assertTrue(formatted.contains("+    added2")) // Plus prefix + 4 spaces from content
        assertTrue(formatted.contains("     context2")) // Space prefix + 4 spaces from content
    }
}
