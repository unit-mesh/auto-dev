package cc.unitmesh.agent.vcs.context

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffContextCompressorTest {
    
    @Test
    fun `should compress simple diff`() {
        val compressor = DiffContextCompressor(
            maxLinesPerFile = 20,
            maxTotalLines = 100
        )
        
        val simpleDiff = """
            --- a/src/main/kotlin/Example.kt
            +++ b/src/main/kotlin/Example.kt
            @@ -1,5 +1,5 @@
             fun hello() {
            -    println("Hello")
            +    println("Hello World")
             }
        """.trimIndent()
        
        val compressed = compressor.compress(simpleDiff)
        
        assertTrue(compressed.contains("Example.kt"))
        assertTrue(compressed.contains("Hello World"))
    }
    
    @Test
    fun `should truncate large file diff`() {
        val compressor = DiffContextCompressor(
            maxLinesPerFile = 10,
            maxTotalLines = 100
        )
        
        // Create a diff with more than 10 lines
        val largeDiff = buildString {
            appendLine("--- a/src/main/kotlin/LargeFile.kt")
            appendLine("+++ b/src/main/kotlin/LargeFile.kt")
            appendLine("@@ -1,50 +1,50 @@")
            repeat(30) { i ->
                appendLine("-    old line $i")
                appendLine("+    new line $i")
            }
        }
        
        val compressed = compressor.compress(largeDiff)
        
        // Should contain truncation notice
        assertTrue(compressed.contains("truncated"))
        // Should be less than original
        assertTrue(compressed.lines().size < largeDiff.lines().size)
    }
    
    @Test
    fun `should prioritize critical files over low priority files`() {
        val compressor = DiffContextCompressor(
            maxLinesPerFile = 50,
            maxTotalLines = 60
        )
        
        val multiFileDiff = """
            --- a/src/main/kotlin/Important.kt
            +++ b/src/main/kotlin/Important.kt
            @@ -1,5 +1,5 @@
             fun important() {
            -    println("old")
            +    println("new")
             }
            
            --- a/data.json
            +++ b/data.json
            @@ -1,5 +1,5 @@
             {
            -    "old": "value"
            +    "new": "value"
             }
        """.trimIndent()
        
        val compressed = compressor.compress(multiFileDiff)
        
        // .kt file (CRITICAL) should be included
        assertTrue(compressed.contains("Important.kt"))
        // May or may not contain .json (LOW priority) depending on space
    }
    
    @Test
    fun `DiffFormatter should process diff`() {
        val rawDiff = """
diff --git a/src/Example.kt b/src/Example.kt
--- a/src/Example.kt
+++ b/src/Example.kt
@@ -1,3 +1,3 @@
 fun test() {
-    old line
+    new line
 }
        """.trimIndent()
        
        val formatted = DiffFormatter.postProcess(rawDiff)
        
        // Just verify it doesn't crash and processes something
        assertTrue(formatted != rawDiff, "Should process the diff")
    }
    
    @Test
    fun `DiffFormatter should detect new file`() {
        val newFileDiff = """
            new file mode 100644
            --- /dev/null
            +++ b/src/NewFile.kt
            fun newFunction() {
                println("new")
            }
        """.trimIndent()
        
        val formatted = DiffFormatter.postProcess(newFileDiff)
        
        assertTrue(formatted.contains("new file"))
        assertTrue(formatted.contains("NewFile.kt"))
    }
    
    @Test
    fun `DiffFormatter should detect deleted file`() {
        val deleteDiff = """
            deleted file mode 100644
            --- a/src/OldFile.kt
            +++ /dev/null
        """.trimIndent()
        
        val formatted = DiffFormatter.postProcess(deleteDiff)
        
        assertTrue(formatted.contains("delete file"))
        assertTrue(formatted.contains("OldFile.kt"))
    }
    
    @Test
    fun `DiffFormatter should detect rename`() {
        val renameDiff = """
            rename from src/Old.kt
            rename to src/New.kt
        """.trimIndent()
        
        val formatted = DiffFormatter.postProcess(renameDiff)
        
        assertTrue(formatted.contains("rename file"))
        assertTrue(formatted.contains("Old.kt"))
        assertTrue(formatted.contains("New.kt"))
    }
}
