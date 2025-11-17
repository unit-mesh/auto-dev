package cc.unitmesh.agent.executor

import cc.unitmesh.agent.ReviewTask
import cc.unitmesh.agent.ReviewType
import cc.unitmesh.devins.workspace.GitDiffFile
import cc.unitmesh.devins.workspace.GitDiffInfo
import cc.unitmesh.devins.workspace.GitFileStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CodeReviewAgentExecutorTest {

    @Test
    fun `should create ReviewTask with GitDiffInfo`() {
        // Given
        val gitDiff = GitDiffInfo(
            files = listOf(
                GitDiffFile(
                    path = "src/main/kotlin/Example.kt",
                    status = GitFileStatus.MODIFIED,
                    additions = 10,
                    deletions = 5,
                    diff = """
                        @@ -1,5 +1,10 @@
                        -fun oldFunction() {
                        -    println("old")
                        +fun newFunction() {
                        +    println("new")
                        +    println("more code")
                        }
                    """.trimIndent()
                )
            ),
            totalAdditions = 10,
            totalDeletions = 5
        )

        // When
        val task = ReviewTask(
            projectPath = "/test/project",
            reviewType = ReviewType.COMPREHENSIVE,
            patch = gitDiff
        )

        // Then
        assertNotNull(task.patch)
        assertEquals(1, task.patch?.files?.size)
        assertEquals(10, task.patch?.totalAdditions)
        assertEquals(5, task.patch?.totalDeletions)
        assertEquals("src/main/kotlin/Example.kt", task.patch?.files?.first()?.path)
    }

    @Test
    fun `should support multiple file changes in GitDiffInfo`() {
        // Given
        val gitDiff = GitDiffInfo(
            files = listOf(
                GitDiffFile(
                    path = "src/main/kotlin/File1.kt",
                    status = GitFileStatus.ADDED,
                    additions = 20,
                    deletions = 0,
                    diff = "@@ -0,0 +1,20 @@\n+new content"
                ),
                GitDiffFile(
                    path = "src/main/kotlin/File2.kt",
                    status = GitFileStatus.DELETED,
                    additions = 0,
                    deletions = 15,
                    diff = "@@ -1,15 +0,0 @@\n-deleted content"
                ),
                GitDiffFile(
                    path = "src/main/kotlin/File3.kt",
                    oldPath = "src/main/kotlin/OldFile3.kt",
                    status = GitFileStatus.RENAMED,
                    additions = 5,
                    deletions = 3,
                    diff = "@@ -1,3 +1,5 @@\n-old\n+new"
                )
            ),
            totalAdditions = 25,
            totalDeletions = 18
        )

        // When
        val task = ReviewTask(
            projectPath = "/test/project",
            reviewType = ReviewType.SECURITY,
            patch = gitDiff
        )

        // Then
        assertNotNull(task.patch)
        assertEquals(3, task.patch?.files?.size)
        assertEquals(25, task.patch?.totalAdditions)
        assertEquals(18, task.patch?.totalDeletions)
        
        val files = task.patch?.files ?: emptyList()
        assertEquals(GitFileStatus.ADDED, files[0].status)
        assertEquals(GitFileStatus.DELETED, files[1].status)
        assertEquals(GitFileStatus.RENAMED, files[2].status)
        assertEquals("src/main/kotlin/OldFile3.kt", files[2].oldPath)
    }
}
