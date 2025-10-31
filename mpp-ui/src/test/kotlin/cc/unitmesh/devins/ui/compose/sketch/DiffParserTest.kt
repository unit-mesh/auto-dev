package cc.unitmesh.devins.ui.compose.sketch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DiffParserTest {
    
    @Test
    fun `should parse standard unified diff`() {
        val diff = """
--- a/File.kt
+++ b/File.kt
@@ -1,3 +1,3 @@
 context line
-deleted line
+added line
 another context
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(diff)
        
        assertEquals(1, fileDiffs.size)
        assertEquals("File.kt", fileDiffs[0].newPath)
        assertEquals("File.kt", fileDiffs[0].oldPath)
        assertEquals(1, fileDiffs[0].hunks.size)
        
        val hunk = fileDiffs[0].hunks[0]
        assertEquals(4, hunk.lines.size)
        assertEquals(DiffLineType.CONTEXT, hunk.lines[0].type)
        assertEquals(DiffLineType.DELETED, hunk.lines[1].type)
        assertEquals(DiffLineType.ADDED, hunk.lines[2].type)
        assertEquals(DiffLineType.CONTEXT, hunk.lines[3].type)
    }
    
    @Test
    fun `should parse multiple hunks`() {
        val diff = """
--- a/File.kt
+++ b/File.kt
@@ -1,3 +1,3 @@
 line 1
-old line
+new line
 line 3
@@ -10,2 +10,3 @@
 line 10
+added line
 line 11
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(diff)
        
        assertEquals(1, fileDiffs.size)
        assertEquals(2, fileDiffs[0].hunks.size)
    }
    
    @Test
    fun `should parse multiple files`() {
        val diff = """
--- a/File1.kt
+++ b/File1.kt
@@ -1,2 +1,2 @@
-old1
+new1
--- a/File2.kt
+++ b/File2.kt
@@ -1,2 +1,2 @@
-old2
+new2
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(diff)
        
        assertEquals(2, fileDiffs.size)
        assertEquals("File1.kt", fileDiffs[0].newPath)
        assertEquals("File2.kt", fileDiffs[1].newPath)
    }
    
    @Test
    fun `should parse new file`() {
        val diff = """
--- /dev/null
+++ b/NewFile.kt
@@ -0,0 +1,3 @@
+line 1
+line 2
+line 3
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(diff)
        
        assertEquals(1, fileDiffs.size)
        assertTrue(fileDiffs[0].isNewFile)
        assertEquals("NewFile.kt", fileDiffs[0].newPath)
    }
    
    @Test
    fun `should parse deleted file`() {
        val diff = """
--- a/OldFile.kt
+++ /dev/null
@@ -1,3 +0,0 @@
-line 1
-line 2
-line 3
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(diff)
        
        assertEquals(1, fileDiffs.size)
        assertTrue(fileDiffs[0].isDeletedFile)
        assertEquals("OldFile.kt", fileDiffs[0].oldPath)
    }
    
    @Test
    fun `should parse diff without file headers`() {
        val diff = """
@@ -1,4 +1,5 @@
 First line
-Second line
+Modified line
+Added line
 Last line
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(diff)
        
        assertEquals(1, fileDiffs.size)
        assertEquals(1, fileDiffs[0].hunks.size)
        assertEquals(5, fileDiffs[0].hunks[0].lines.size)
    }
    
    @Test
    fun `should track line numbers correctly`() {
        val diff = """
@@ -10,5 +10,6 @@
 line 10
 line 11
-line 12 deleted
+line 12 modified
+line 13 added
 line 14
 line 15
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(diff)
        val lines = fileDiffs[0].hunks[0].lines
        
        // 检查旧行号
        assertEquals(10, lines[0].oldLineNumber)
        assertEquals(11, lines[1].oldLineNumber)
        assertEquals(12, lines[2].oldLineNumber)
        
        // 检查新行号
        assertEquals(10, lines[0].newLineNumber)
        assertEquals(11, lines[1].newLineNumber)
        assertEquals(12, lines[3].newLineNumber) // added line
        assertEquals(13, lines[4].newLineNumber) // added line
    }
    
    @Test
    fun `should handle empty lines in diff`() {
        val diff = """
@@ -1,3 +1,3 @@
 line 1

 line 3
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(diff)
        
        assertNotNull(fileDiffs)
        assertEquals(1, fileDiffs.size)
    }
}


