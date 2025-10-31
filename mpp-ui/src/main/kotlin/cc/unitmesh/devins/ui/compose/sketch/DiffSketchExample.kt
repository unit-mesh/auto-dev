package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * DiffSketch 使用示例
 * 
 * ## 使用方式
 * 
 * ### 1. 直接使用 DiffSketchRenderer
 * ```kotlin
 * DiffSketchRenderer.RenderDiff(
 *     diffContent = diffText,
 *     onAccept = { /* 接受修改 */ },
 *     onReject = { /* 拒绝修改 */ }
 * )
 * ```
 * 
 * ### 2. 通过 SketchRenderer（推荐）
 * ```kotlin
 * SketchRenderer.RenderResponse(
 *     content = """
 *     这是一些修改：
 *     ```diff
 *     --- a/file.kt
 *     +++ b/file.kt
 *     @@ -1,3 +1,3 @@
 *      fun example() {
 *     -    println("old")
 *     +    println("new")
 *      }
 *     ```
 *     """,
 *     isComplete = true
 * )
 * ```
 * 
 * ## 特性
 * 
 * - ✅ 支持标准 Unified Diff 格式
 * - ✅ 自动折叠/展开大量行（默认显示5行）
 * - ✅ 双列行号（旧行号 + 新行号）
 * - ✅ 添加行（绿色）和删除行（红色）高亮
 * - ✅ 多文件 diff 支持
 * - ✅ Accept/Reject 操作按钮
 * - ✅ 文件统计信息（+/-行数）
 */
@Composable
fun DiffSketchExampleScreen() {
    val sampleDiff = """
--- a/src/main/kotlin/Example.kt
+++ b/src/main/kotlin/Example.kt
@@ -1,10 +1,12 @@
 package com.example
 
 class Example {
-    fun oldMethod() {
-        println("This is old")
+    fun newMethod() {
+        println("This is new")
+        println("Added another line")
     }
     
     fun unchanged() {
         println("This remains the same")
     }
+    
+    fun additionalMethod() {
+        println("Brand new method")
+    }
 }
    """.trimIndent()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        DiffSketchRenderer.RenderDiff(
            diffContent = sampleDiff,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            onAccept = {
                println("✅ User accepted the diff")
            },
            onReject = {
                println("❌ User rejected the diff")
            }
        )
    }
}

/**
 * 多文件 Diff 示例
 */
@Composable
fun MultiFileDiffExample() {
    val multiFileDiff = """
--- a/File1.kt
+++ b/File1.kt
@@ -1,3 +1,3 @@
 fun file1() {
-    println("old")
+    println("new")
 }
--- a/File2.kt
+++ b/File2.kt
@@ -1,5 +1,6 @@
 fun file2() {
     println("line 1")
+    println("added line")
     println("line 2")
 }
    """.trimIndent()
    
    DiffSketchRenderer.RenderDiff(
        diffContent = multiFileDiff,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    )
}

/**
 * 简化格式 Diff（不带文件头）
 */
@Composable
fun SimpleDiffExample() {
    val simpleDiff = """
@@ -1,4 +1,5 @@
 First line
-Second line (removed)
+Second line (modified)
+Third line (added)
 Fourth line
    """.trimIndent()
    
    DiffSketchRenderer.RenderDiff(
        diffContent = simpleDiff,
        modifier = Modifier.padding(16.dp)
    )
}

/**
 * Git Diff 格式示例（包含 diff --git 和 index）
 */
@Composable
fun GitDiffFormatExample() {
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
    
    DiffSketchRenderer.RenderDiff(
        diffContent = gitDiff,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        onAccept = {
            println("✅ Git diff accepted")
        },
        onReject = {
            println("❌ Git diff rejected")
        }
    )
}

/**
 * 二进制文件 Diff
 */
@Composable
fun BinaryFileDiffExample() {
    val binaryDiff = """
diff --git a/image.png b/image.png
index abc123..def456 100644
Binary files a/image.png and b/image.png differ
    """.trimIndent()
    
    DiffSketchRenderer.RenderDiff(
        diffContent = binaryDiff,
        modifier = Modifier.padding(16.dp)
    )
}

/**
 * 文件模式变更示例
 */
@Composable
fun ModeChangeDiffExample() {
    val modeChangeDiff = """
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
    
    DiffSketchRenderer.RenderDiff(
        diffContent = modeChangeDiff,
        modifier = Modifier.padding(16.dp)
    )
}

