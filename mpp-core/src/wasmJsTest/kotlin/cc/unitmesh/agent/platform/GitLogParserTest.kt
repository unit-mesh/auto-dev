package cc.unitmesh.agent.platform

import cc.unitmesh.devins.workspace.GitCommitInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitLogParserTest {

    @Test
    fun testParseSingleCommit() {
        val logOutput = """
commit ccd7e7bfa0778fc700a2c1b7ebb6c8c90d6dbc4b (HEAD -> master)
Author: Phodal Huang <h@phodal.com>
Date:   Tue Nov 18 15:33:49 2025 +0800

    feat(wasm-git): add WASM Git clone UI and view model

    Introduce WasmGitCloneScreen and WasmGitViewModel for handling Git clone operations in the WASM UI. Update related components and GitOperations platform implementations to support this feature.
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        val commit = commits[0]
        assertEquals("ccd7e7bfa0778fc700a2c1b7ebb6c8c90d6dbc4b", commit.hash)
        assertEquals("Phodal Huang", commit.author)
        assertEquals("h@phodal.com", commit.email)
        assertTrue(commit.date > 0)
        assertTrue(commit.message.contains("feat(wasm-git): add WASM Git clone UI and view model"))
        assertTrue(commit.message.contains("Introduce WasmGitCloneScreen"))
    }

    @Test
    fun testParseMultipleCommits() {
        val logOutput = """
commit ccd7e7bfa0778fc700a2c1b7ebb6c8c90d6dbc4b (HEAD -> master)
Author: Phodal Huang <h@phodal.com>
Date:   Tue Nov 18 15:33:49 2025 +0800

    feat(wasm-git): add WASM Git clone UI and view model

    Introduce WasmGitCloneScreen and WasmGitViewModel for handling Git clone operations in the WASM UI. Update related components and GitOperations platform implementations to support this feature.

commit 2545b1df5e7861918e862f2f092ca7cd74c2fc57
Author: Phodal Huang <h@phodal.com>
Date:   Tue Nov 18 14:43:43 2025 +0800

    feat(ui): add back button to CodeReviewPage on WASM

    Show a navigation back button in the code review top bar when running on the WASM platform, allowing users to return to the coding agent.

commit 23a33773e4194a094d76968fe0f748eb882bb4a7
Author: Phodal Huang <h@phodal.com>
Date:   Tue Nov 18 14:36:07 2025 +0800

    feat(wasm-git): enable async WASM interop and stdout capture

    Switch to async wasm-git (lg2_async), update interop for Promise-based API, capture stdout from git commands, and improve webpack config for browser compatibility.
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(3, commits.size)

        // First commit
        assertEquals("ccd7e7bfa0778fc700a2c1b7ebb6c8c90d6dbc4b", commits[0].hash)
        assertEquals("Phodal Huang", commits[0].author)
        assertEquals("h@phodal.com", commits[0].email)
        assertTrue(commits[0].message.contains("feat(wasm-git): add WASM Git clone UI and view model"))

        // Second commit
        assertEquals("2545b1df5e7861918e862f2f092ca7cd74c2fc57", commits[1].hash)
        assertEquals("Phodal Huang", commits[1].author)
        assertTrue(commits[1].message.contains("feat(ui): add back button to CodeReviewPage on WASM"))

        // Third commit
        assertEquals("23a33773e4194a094d76968fe0f748eb882bb4a7", commits[2].hash)
        assertEquals("Phodal Huang", commits[2].author)
        assertTrue(commits[2].message.contains("feat(wasm-git): enable async WASM interop and stdout capture"))
    }

    @Test
    fun testParseCommitWithoutRefs() {
        val logOutput = """
commit 2545b1df5e7861918e862f2f092ca7cd74c2fc57
Author: Phodal Huang <h@phodal.com>
Date:   Tue Nov 18 14:43:43 2025 +0800

    feat(ui): add back button to CodeReviewPage on WASM

    Show a navigation back button in the code review top bar when running on the WASM platform.
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertEquals("2545b1df5e7861918e862f2f092ca7cd74c2fc57", commits[0].hash)
        assertEquals("Phodal Huang", commits[0].author)
        assertEquals("h@phodal.com", commits[0].email)
        assertTrue(commits[0].message.contains("feat(ui): add back button to CodeReviewPage on WASM"))
    }

    @Test
    fun testParseCommitWithSingleLineMessage() {
        val logOutput = """
commit abc123def456
Author: John Doe <john@example.com>
Date:   Mon Jan 1 12:00:00 2024 +0000

    fix: simple bug fix
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertEquals("abc123def456", commits[0].hash)
        assertEquals("John Doe", commits[0].author)
        assertEquals("john@example.com", commits[0].email)
        assertEquals("fix: simple bug fix", commits[0].message)
    }

    @Test
    fun testParseCommitWithIssueNumber() {
        val logOutput = """
commit ccd7e7bfa0778fc700a2c1b7ebb6c8c90d6dbc4b (HEAD -> master)
Author: Phodal Huang <h@phodal.com>
Date:   Tue Nov 18 15:33:49 2025 +0800

    feat(wasm-git): add WASM Git clone UI and view model #453

    Introduce WasmGitCloneScreen and WasmGitViewModel for handling Git clone operations in the WASM UI.
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertTrue(commits[0].message.contains("#453"))
    }

    @Test
    fun testParseDifferentAuthors() {
        val logOutput = """
commit abc123
Author: Alice Smith <alice@example.com>
Date:   Mon Jan 1 10:00:00 2024 +0000

    Initial commit

commit def456
Author: Bob Johnson <bob@example.org>
Date:   Mon Jan 1 11:00:00 2024 +0000

    Second commit
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(2, commits.size)
        assertEquals("Alice Smith", commits[0].author)
        assertEquals("alice@example.com", commits[0].email)
        assertEquals("Bob Johnson", commits[1].author)
        assertEquals("bob@example.org", commits[1].email)
    }

    @Test
    fun testParseDifferentTimezones() {
        val logOutput = """
commit abc123
Author: Test User <test@example.com>
Date:   Tue Nov 18 15:33:49 2025 +0800

    Commit in UTC+8

commit def456
Author: Test User <test@example.com>
Date:   Tue Nov 18 14:43:43 2025 -0500

    Commit in UTC-5
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(2, commits.size)
        assertTrue(commits[0].date > 0)
        assertTrue(commits[1].date > 0)
    }

    @Test
    fun testParseEmptyLog() {
        val logOutput = ""

        val commits = GitLogParser.parse(logOutput)

        assertEquals(0, commits.size)
    }

    @Test
    fun testParseBlankLog() {
        val logOutput = "   \n  \n  "

        val commits = GitLogParser.parse(logOutput)

        assertEquals(0, commits.size)
    }

    @Test
    fun testParseCommitWithMultipleBranches() {
        val logOutput = """
commit abc123def (HEAD -> main, origin/main, origin/HEAD, tag: v1.0.0)
Author: Developer <dev@example.com>
Date:   Mon Jan 1 12:00:00 2024 +0000

    Release version 1.0.0
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertEquals("abc123def", commits[0].hash)
        assertEquals("Developer", commits[0].author)
    }

    @Test
    fun testParseAuthorNameWithSpecialCharacters() {
        val logOutput = """
commit abc123
Author: O'Brien Jr. <obrien@example.com>
Date:   Mon Jan 1 12:00:00 2024 +0000

    Test commit
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertEquals("O'Brien Jr.", commits[0].author)
        assertEquals("obrien@example.com", commits[0].email)
    }

    @Test
    fun testParseCommitWithLongHash() {
        val logOutput = """
commit ccd7e7bfa0778fc700a2c1b7ebb6c8c90d6dbc4b1234567890abcdef
Author: Test <test@example.com>
Date:   Mon Jan 1 12:00:00 2024 +0000

    Test with long hash
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertEquals("ccd7e7bfa0778fc700a2c1b7ebb6c8c90d6dbc4b1234567890abcdef", commits[0].hash)
    }

    @Test
    fun testParseCommitWithShortHash() {
        val logOutput = """
commit abc123
Author: Test <test@example.com>
Date:   Mon Jan 1 12:00:00 2024 +0000

    Test with short hash
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertEquals("abc123", commits[0].hash)
    }

    @Test
    fun testParseMessageWithEmptyLines() {
        val logOutput = """
commit abc123
Author: Test <test@example.com>
Date:   Mon Jan 1 12:00:00 2024 +0000

    First paragraph

    Second paragraph after empty line

    Third paragraph
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertTrue(commits[0].message.contains("First paragraph"))
        assertTrue(commits[0].message.contains("Second paragraph"))
        assertTrue(commits[0].message.contains("Third paragraph"))
    }

    @Test
    fun testParseMessageWithBulletPoints() {
        val logOutput = """
commit abc123
Author: Test <test@example.com>
Date:   Mon Jan 1 12:00:00 2024 +0000

    feat: add new features

    - Feature 1
    - Feature 2
    - Feature 3
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertTrue(commits[0].message.contains("- Feature 1"))
        assertTrue(commits[0].message.contains("- Feature 2"))
        assertTrue(commits[0].message.contains("- Feature 3"))
    }

    @Test
    fun testParseMessageWithIndentedCode() {
        val logOutput = """
commit abc123
Author: Test <test@example.com>
Date:   Mon Jan 1 12:00:00 2024 +0000

    fix: update configuration

        const config = {
            enabled: true
        };
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertTrue(commits[0].message.contains("const config"))
    }

    @Test
    fun testParseDifferentMonths() {
        val logOutput = """
commit abc1
Author: Test <test@example.com>
Date:   Mon Jan 15 12:00:00 2024 +0000

    January

commit abc2
Author: Test <test@example.com>
Date:   Thu Feb 14 12:00:00 2024 +0000

    February

commit abc3
Author: Test <test@example.com>
Date:   Fri Dec 25 12:00:00 2024 +0000

    December
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(3, commits.size)
        // Verify all commits are parsed
        commits.forEach { commit ->
            assertTrue(commit.date > 0)
            assertNotNull(commit.hash)
        }
    }

    @Test
    fun testParseLeapYearDate() {
        val logOutput = """
commit abc123
Author: Test <test@example.com>
Date:   Thu Feb 29 12:00:00 2024 +0000

    Leap year commit
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertTrue(commits[0].date > 0)
    }

    @Test
    fun testParseCommitWithoutMessage() {
        // Edge case: commit with no message (shouldn't happen but let's be safe)
        val logOutput = """
commit abc123
Author: Test <test@example.com>
Date:   Mon Jan 1 12:00:00 2024 +0000

        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertEquals("abc123", commits[0].hash)
        assertEquals("", commits[0].message.trim())
    }

    @Test
    fun testParseMessageNotIndented() {
        // Some git log outputs might not indent messages
        val logOutput = """
commit abc123
Author: Test <test@example.com>
Date:   Mon Jan 1 12:00:00 2024 +0000

This is a message without indentation
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(1, commits.size)
        assertTrue(commits[0].message.contains("This is a message without indentation"))
    }

    @Test
    fun testRealWorldExample() {
        // Exact example from user's request
        val logOutput = """
commit ccd7e7bfa0778fc700a2c1b7ebb6c8c90d6dbc4b (HEAD -> master)
Author: Phodal Huang <h@phodal.com>
Date:   Tue Nov 18 15:33:49 2025 +0800

    feat(wasm-git): add WASM Git clone UI and view model #453

    Introduce WasmGitCloneScreen and WasmGitViewModel for handling Git clone operations in the WASM UI. Update related components and GitOperations platform implementations to support this feature.

commit 2545b1df5e7861918e862f2f092ca7cd74c2fc57
Author: Phodal Huang <h@phodal.com>
Date:   Tue Nov 18 14:43:43 2025 +0800

    feat(ui): add back button to CodeReviewPage on WASM #453

    Show a navigation back button in the code review top bar when running on the WASM platform, allowing users to return to the coding agent.

commit 23a33773e4194a094d76968fe0f748eb882bb4a7
Author: Phodal Huang <h@phodal.com>
Date:   Tue Nov 18 14:36:07 2025 +0800

    feat(wasm-git): enable async WASM interop and stdout capture #453

    Switch to async wasm-git (lg2_async), update interop for Promise-based API, capture stdout from git commands, and improve webpack config for browser compatibility.
        """.trimIndent()

        val commits = GitLogParser.parse(logOutput)

        assertEquals(3, commits.size)

        // Verify all commits have required fields
        commits.forEach { commit ->
            assertTrue(commit.hash.isNotEmpty(), "Hash should not be empty")
            assertTrue(commit.author.isNotEmpty(), "Author should not be empty")
            assertTrue(commit.email.isNotEmpty(), "Email should not be empty")
            assertTrue(commit.date > 0, "Date should be positive")
            assertTrue(commit.message.isNotEmpty(), "Message should not be empty")
        }

        // Verify specific content
        assertEquals("ccd7e7bfa0778fc700a2c1b7ebb6c8c90d6dbc4b", commits[0].hash)
        assertEquals("Phodal Huang", commits[0].author)
        assertEquals("h@phodal.com", commits[0].email)
        assertTrue(commits[0].message.startsWith("feat(wasm-git): add WASM Git clone UI and view model #453"))

        assertEquals("2545b1df5e7861918e862f2f092ca7cd74c2fc57", commits[1].hash)
        assertTrue(commits[1].message.startsWith("feat(ui): add back button to CodeReviewPage on WASM #453"))

        assertEquals("23a33773e4194a094d76968fe0f748eb882bb4a7", commits[2].hash)
        assertTrue(commits[2].message.startsWith("feat(wasm-git): enable async WASM interop and stdout capture #453"))
    }
}

