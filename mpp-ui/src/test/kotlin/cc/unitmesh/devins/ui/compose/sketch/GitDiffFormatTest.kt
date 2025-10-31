package cc.unitmesh.devins.ui.compose.sketch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 测试 Git Diff 格式支持
 */
class GitDiffFormatTest {
    
    @Test
    fun `should parse git diff format with diff --git header`() {
        val gitDiff = """
diff --git a/src/main/java/com/example/App.java b/src/main/java/com/example/App.java
index e69de29..4b825dc 100644
--- a/src/main/java/com/example/App.java
+++ b/src/main/java/com/example/App.java
@@ -1,7 +1,7 @@
 public class App {
-    public String greet() {
-        return "Hello, world!";
-    }
+    public String greet() {
+        return "Hello, GNU patch!";
+    }
 
     public static void main(String[] args) {
         System.out.println(new App().greet());
     }
 }
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(gitDiff)
        
        assertNotNull(fileDiffs)
        assertEquals(1, fileDiffs.size)
        
        val fileDiff = fileDiffs[0]
        assertEquals("src/main/java/com/example/App.java", fileDiff.oldPath)
        assertEquals("src/main/java/com/example/App.java", fileDiff.newPath)
        assertEquals(1, fileDiff.hunks.size)
        
        val hunk = fileDiff.hunks[0]
        assertEquals(1, hunk.oldStartLine)
        assertEquals(1, hunk.newStartLine)
        
        // 验证行内容
        val deletedLines = hunk.lines.filter { it.type == DiffLineType.DELETED }
        val addedLines = hunk.lines.filter { it.type == DiffLineType.ADDED }
        
        assertEquals(3, deletedLines.size)
        assertEquals(3, addedLines.size)
    }
    
    @Test
    fun `should parse multiple files in git diff format`() {
        val gitDiff = """
diff --git a/File1.kt b/File1.kt
index abc123..def456 100644
--- a/File1.kt
+++ b/File1.kt
@@ -1,2 +1,2 @@
-old line 1
+new line 1
diff --git a/File2.kt b/File2.kt
index 111222..333444 100644
--- a/File2.kt
+++ b/File2.kt
@@ -1,2 +1,2 @@
-old line 2
+new line 2
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(gitDiff)
        
        assertEquals(2, fileDiffs.size)
        assertEquals("File1.kt", fileDiffs[0].oldPath)
        assertEquals("File2.kt", fileDiffs[1].oldPath)
    }
    
    @Test
    fun `should parse new file in git diff format`() {
        val gitDiff = """
diff --git a/NewFile.kt b/NewFile.kt
new file mode 100644
index 0000000..abc1234
--- /dev/null
+++ b/NewFile.kt
@@ -0,0 +1,3 @@
+line 1
+line 2
+line 3
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(gitDiff)
        
        assertEquals(1, fileDiffs.size)
        assertEquals(true, fileDiffs[0].isNewFile)
        assertEquals("NewFile.kt", fileDiffs[0].newPath)
    }
    
    @Test
    fun `should parse deleted file in git diff format`() {
        val gitDiff = """
diff --git a/OldFile.kt b/OldFile.kt
deleted file mode 100644
index abc1234..0000000
--- a/OldFile.kt
+++ /dev/null
@@ -1,3 +0,0 @@
-line 1
-line 2
-line 3
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(gitDiff)
        
        assertEquals(1, fileDiffs.size)
        assertEquals(true, fileDiffs[0].isDeletedFile)
        assertEquals("OldFile.kt", fileDiffs[0].oldPath)
    }
    
    @Test
    fun `should parse binary file indication`() {
        val gitDiff = """
diff --git a/image.png b/image.png
index abc123..def456 100644
Binary files a/image.png and b/image.png differ
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(gitDiff)
        
        // 应该能识别文件，但不会有 hunks（因为是二进制文件）
        assertEquals(1, fileDiffs.size)
        assertEquals("image.png", fileDiffs[0].oldPath)
        assertEquals(true, fileDiffs[0].isBinaryFile)
    }
    
    @Test
    fun `should handle git diff with mode changes`() {
        val gitDiff = """
diff --git a/script.sh b/script.sh
old mode 100644
new mode 100755
index abc123..abc123 100644
--- a/script.sh
+++ b/script.sh
@@ -1,3 +1,3 @@
 #!/bin/bash
-echo "old"
+echo "new"
        """.trimIndent()
        
        val fileDiffs = DiffParser.parse(gitDiff)
        
        assertEquals(1, fileDiffs.size)
        assertEquals("script.sh", fileDiffs[0].oldPath)
        // 模式变化会被记录在 metadata 中
        assertEquals("100755", fileDiffs[0].newMode)
    }
}

