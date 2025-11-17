package cc.unitmesh.agent.executor

import cc.unitmesh.agent.ReviewTask
import cc.unitmesh.agent.ReviewType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CodeReviewAgentExecutorTest {

    @Test
    fun `should create ReviewTask with patch string`() {
        // Given
        val patch = """
            --- a/src/main/kotlin/Example.kt
            +++ b/src/main/kotlin/Example.kt
            @@ -1,5 +1,10 @@
            -fun oldFunction() {
            -    println("old")
            +fun newFunction() {
            +    println("new")
            +    println("more code")
            }
        """.trimIndent()

        // When
        val task = ReviewTask(
            projectPath = "/test/project",
            reviewType = ReviewType.COMPREHENSIVE,
            patch = patch
        )

        // Then
        assertNotNull(task.patch)
        assertEquals(patch, task.patch)
    }

    @Test
    fun `should support multiple file changes in patch`() {
        // Given
        val patch = """
            --- a/src/main/kotlin/File1.kt
            +++ b/src/main/kotlin/File1.kt
            @@ -0,0 +1,20 @@
            +new content
            
            --- a/src/main/kotlin/File2.kt
            +++ /dev/null
            @@ -1,15 +0,0 @@
            -deleted content
            
            rename from src/main/kotlin/OldFile3.kt
            rename to src/main/kotlin/File3.kt
        """.trimIndent()

        // When
        val task = ReviewTask(
            projectPath = "/test/project",
            reviewType = ReviewType.SECURITY,
            patch = patch
        )

        // Then
        assertNotNull(task.patch)
        assertEquals(patch, task.patch)
    }
}
