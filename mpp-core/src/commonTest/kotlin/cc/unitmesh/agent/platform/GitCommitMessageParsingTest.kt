package cc.unitmesh.agent.platform

import cc.unitmesh.devins.workspace.GitCommitInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for Git commit message parsing logic
 * 
 * These tests validate the parsing format used in GitOperations:
 * - Format: %H%x1f%an%x1f%ae%x1f%ct%x1f%B%x00
 * - Delimiter: \u001f (Unit Separator, ASCII 31) between fields
 * - Separator: \u0000 (Null, ASCII 0) between records
 */
class GitCommitMessageParsingTest {
    
    @Test
    fun `test parse simple single-line message`() {
        val record = "abc123def456\u001fJohn Doe\u001fjohn@example.com\u001f1700000000\u001ffix: simple bug fix"
        val commit = parseGitCommitRecord(record)
        
        assertEquals("abc123def456", commit.hash)
        assertEquals("John Doe", commit.author)
        assertEquals("john@example.com", commit.email)
        assertEquals(1700000000L, commit.date)
        assertEquals("fix: simple bug fix", commit.message)
        assertEquals("abc123d", commit.shortHash)
    }
    
    @Test
    fun `test parse multi-line message with body`() {
        val message = """feat: add new feature #123

This is a detailed description of the feature.
It spans multiple lines and provides context.

- Added functionality A
- Fixed issue B"""
        
        val record = "def456abc789\u001fJane Smith\u001fjane@example.com\u001f1700100000\u001f$message"
        val commit = parseGitCommitRecord(record)
        
        assertEquals("def456abc789", commit.hash)
        assertEquals("Jane Smith", commit.author)
        assertTrue(commit.message.contains("feat: add new feature #123"))
        assertTrue(commit.message.contains("This is a detailed description"))
        assertTrue(commit.message.contains("Added functionality A"))
        
        val lines = commit.message.lines()
        assertTrue(lines.size > 1, "Should have multiple lines")
        assertEquals("feat: add new feature #123", lines[0])
    }
    
    @Test
    fun `test parse message with pipe characters`() {
        val message = "fix: handle A|B|C cases\n\nSupport pipe-separated values (A|B|C) in parser."
        val record = "111222333444\u001fBob Johnson\u001fbob@test.com\u001f1700200000\u001f$message"
        val commit = parseGitCommitRecord(record)
        
        assertTrue(commit.message.contains("A|B|C"))
        assertTrue(commit.message.contains("pipe-separated values"))
        assertEquals("Bob Johnson", commit.author)
    }
    
    @Test
    fun `test parse message with special characters`() {
        val message = """feat: add emoji support 🎉

Implemented Unicode support including:
- Emojis (🎉 🚀 ✨)
- Special chars (<>&"')
- Accented letters (café, naïve)"""
        
        val record = "555666777888\u001fAlice Chen\u001falice@dev.io\u001f1700300000\u001f$message"
        val commit = parseGitCommitRecord(record)
        
        assertTrue(commit.message.contains("emoji support"))
        assertTrue(commit.message.contains("🎉"))
        assertTrue(commit.message.contains("Special chars"))
        assertTrue(commit.message.contains("café"))
    }
    
    @Test
    fun `test parse message with blank lines`() {
        val message = """refactor(ui): remove component #463

Deleted the SimpleGitGraphColumn composable.


Also simplified LazyColumn usage."""
        
        val record = "999888777666\u001fDev Team\u001fteam@company.com\u001f1700400000\u001f$message"
        val commit = parseGitCommitRecord(record)
        
        assertTrue(commit.message.contains("remove component #463"))
        assertTrue(commit.message.contains("Deleted the SimpleGitGraphColumn"))
        assertTrue(commit.message.contains("simplified LazyColumn"))
        
        // Verify blank lines are preserved
        val normalized = commit.message.replace("\r\n", "\n")
        assertTrue(normalized.contains("\n\n"), "Should preserve blank lines between paragraphs")
    }
    
    @Test
    fun `test parse multiple records`() {
        val record1 = "aaa111\u001fAuthor1\u001fauthor1@test.com\u001f1000\u001fFirst commit"
        val record2 = "bbb222\u001fAuthor2\u001fauthor2@test.com\u001f2000\u001fSecond commit\nwith body"
        val record3 = "ccc333\u001fAuthor3\u001fauthor3@test.com\u001f3000\u001fThird commit"
        
        val records = "$record1\u0000$record2\u0000$record3"
        
        val commits = records.split("\u0000")
            .filter { it.isNotBlank() }
            .map { parseGitCommitRecord(it.trim()) }
        
        assertEquals(3, commits.size)
        assertEquals("First commit", commits[0].message.trim())
        assertTrue(commits[1].message.contains("Second commit"))
        assertTrue(commits[1].message.contains("with body"))
        assertEquals("Third commit", commits[2].message.trim())
    }
    
    @Test
    fun `test parse message with trailing newlines`() {
        val message = "chore: update dependencies\n\n"
        val record = "deadbeef\u001fMaintainer\u001fmaint@proj.org\u001f1700500000\u001f$message"
        val commit = parseGitCommitRecord(record)
        
        // Message should be trimmed
        assertEquals("chore: update dependencies", commit.message.trim())
    }
    
    @Test
    fun `test parse real-world conventional commit`() {
        val message = """refactor(ui): remove unused SimpleGitGraphColumn component #463

Deleted the SimpleGitGraphColumn composable and related preview code. Also simplified LazyColumn usage and cleaned up formatting in GitGraphColumn and CommitListView.

Breaking changes: None
Co-authored-by: Developer <dev@example.com>"""
        
        val record = "1a2b3c4d5e6f\u001fJohn Developer\u001fjohn.dev@company.com\u001f1732291200\u001f$message"
        val commit = parseGitCommitRecord(record)
        
        // Verify all parts are captured
        assertTrue(commit.message.contains("refactor(ui): remove unused SimpleGitGraphColumn component #463"))
        assertTrue(commit.message.contains("Deleted the SimpleGitGraphColumn composable"))
        assertTrue(commit.message.contains("Breaking changes: None"))
        assertTrue(commit.message.contains("Co-authored-by:"))
        
        // Verify structure
        val lines = commit.message.lines()
        assertEquals("refactor(ui): remove unused SimpleGitGraphColumn component #463", lines[0])
        assertTrue(lines.size > 3, "Should have subject, blank line, and body")
    }
    
    /**
     * Simulates the parsing logic used in GitOperations.jvm.kt and GitOperations.js.kt
     */
    private fun parseGitCommitRecord(record: String): GitCommitInfo {
        val parts = record.split("\u001f")
        require(parts.size >= 5) { "Invalid record format, expected at least 5 parts but got ${parts.size}" }
        
        return GitCommitInfo(
            hash = parts[0],
            author = parts[1],
            email = parts[2],
            date = parts[3].toLongOrNull() ?: 0L,
            message = parts[4].trim(),
            shortHash = parts[0].take(7)
        )
    }
}

