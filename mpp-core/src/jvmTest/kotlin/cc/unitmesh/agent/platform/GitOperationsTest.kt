package cc.unitmesh.agent.platform

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test GitOperations with focus on commit message parsing
 * 
 * Tests that commit messages are fully retrieved including:
 * - Subject (first line)
 * - Body (subsequent lines)
 * - Multi-line messages with blank lines
 */
class GitOperationsTest {
    
    private lateinit var tempDir: File
    private lateinit var gitOps: GitOperations
    
    @Before
    fun setup() {
        // Create a temporary git repository for testing
        tempDir = createTempDir("git-test")
        gitOps = GitOperations(tempDir.absolutePath)
        
        // Initialize git repo
        execCommand(tempDir, "git", "init")
        execCommand(tempDir, "git", "config", "user.name", "Test User")
        execCommand(tempDir, "git", "config", "user.email", "test@example.com")
    }
    
    @Test
    fun `test parse commit with multi-line message`() = runBlocking {
        // Create a commit with multi-line message
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Hello World")
        execCommand(tempDir, "git", "add", "test.txt")
        
        val commitMessage = """
            feat: add new feature #123
            
            This is a detailed description of the feature.
            It spans multiple lines and provides context.
            
            - Added functionality A
            - Fixed issue B
            - Improved performance
        """.trimIndent()
        
        execCommand(tempDir, "git", "commit", "-m", commitMessage)
        
        // Get recent commits
        val commits = gitOps.getRecentCommits(1)
        
        // Verify
        assertEquals(1, commits.size, "Should have 1 commit")
        val commit = commits[0]
        
        assertNotNull(commit.hash, "Commit hash should not be null")
        assertEquals("Test User", commit.author)
        assertEquals("test@example.com", commit.email)
        
        // Most important: verify the full message is retrieved
        assertTrue(
            commit.message.contains("feat: add new feature #123"),
            "Message should contain subject line"
        )
        assertTrue(
            commit.message.contains("This is a detailed description"),
            "Message should contain body description"
        )
        assertTrue(
            commit.message.contains("Added functionality A"),
            "Message should contain bullet points"
        )
        
        // Verify the message structure
        val lines = commit.message.lines()
        assertTrue(lines.size > 1, "Message should have multiple lines")
        assertEquals("feat: add new feature #123", lines[0].trim())
    }
    
    @Test
    fun `test parse commit with subject only`() = runBlocking {
        // Create a commit with single-line message
        val testFile = File(tempDir, "test2.txt")
        testFile.writeText("Test content")
        execCommand(tempDir, "git", "add", "test2.txt")
        execCommand(tempDir, "git", "commit", "-m", "fix: simple fix #456")
        
        val commits = gitOps.getRecentCommits(1)
        
        assertEquals(1, commits.size)
        val commit = commits[0]
        
        assertEquals("fix: simple fix #456", commit.message.trim())
    }
    
    @Test
    fun `test parse multiple commits with varied messages`() = runBlocking {
        // Create multiple commits with different message styles
        val messages = listOf(
            "refactor(ui): remove unused component #463\n\nDeleted the SimpleGitGraphColumn composable and related preview code.",
            "docs: update README",
            "feat: add new API\n\nImplemented REST endpoint for user management.\n\nBreaking changes:\n- Old endpoint deprecated\n- New auth required"
        )
        
        messages.forEach { msg ->
            val file = File(tempDir, "file${messages.indexOf(msg)}.txt")
            file.writeText("content ${messages.indexOf(msg)}")
            execCommand(tempDir, "git", "add", file.name)
            execCommand(tempDir, "git", "commit", "-m", msg)
        }
        
        val commits = gitOps.getRecentCommits(3)
        
        assertEquals(3, commits.size, "Should retrieve 3 commits")
        
        // Verify first commit (most recent)
        assertTrue(
            commits[0].message.contains("Breaking changes"),
            "Most recent commit should contain full body"
        )
        
        // Verify middle commit
        assertEquals("docs: update README", commits[1].message.trim())
        
        // Verify oldest commit
        assertTrue(
            commits[2].message.contains("remove unused component #463"),
            "Should contain subject"
        )
        assertTrue(
            commits[2].message.contains("Deleted the SimpleGitGraphColumn"),
            "Should contain body"
        )
    }
    
    @Test
    fun `test commit message with pipe character`() = runBlocking {
        // Test that pipe characters in commit message don't break parsing
        val testFile = File(tempDir, "test3.txt")
        testFile.writeText("Test")
        execCommand(tempDir, "git", "add", "test3.txt")
        
        val messageWithPipe = "fix: handle A|B|C cases\n\nSupport pipe-separated values (A|B|C) in parser."
        execCommand(tempDir, "git", "commit", "-m", messageWithPipe)
        
        val commits = gitOps.getRecentCommits(1)
        
        assertEquals(1, commits.size)
        assertTrue(
            commits[0].message.contains("A|B|C"),
            "Pipe characters should be preserved in message"
        )
        assertTrue(
            commits[0].message.contains("pipe-separated values"),
            "Full message with pipes should be retrieved"
        )
    }
    
    @Test
    fun `test commit message with special characters`() = runBlocking {
        // Test various special characters
        val testFile = File(tempDir, "test4.txt")
        testFile.writeText("Test")
        execCommand(tempDir, "git", "add", "test4.txt")
        
        val specialMessage = """
            feat: add emoji support 🎉
            
            Implemented Unicode support including:
            - Emojis (🎉 🚀 ✨)
            - Special chars (<>&"')
            - Accented letters (café, naïve)
        """.trimIndent()
        
        execCommand(tempDir, "git", "commit", "-m", specialMessage)
        
        val commits = gitOps.getRecentCommits(1)
        
        assertEquals(1, commits.size)
        val message = commits[0].message
        
        assertTrue(message.contains("emoji support"), "Should contain subject")
        assertTrue(message.contains("Unicode support"), "Should contain body")
        assertTrue(message.contains("Special chars"), "Should preserve special characters")
    }
    
    private fun execCommand(workDir: File, vararg command: String): String {
        val process = ProcessBuilder(*command)
            .directory(workDir)
            .redirectErrorStream(true)
            .start()
        
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        
        if (exitCode != 0) {
            throw RuntimeException("Command failed: ${command.joinToString(" ")}\nOutput: $output")
        }
        
        return output
    }
}

