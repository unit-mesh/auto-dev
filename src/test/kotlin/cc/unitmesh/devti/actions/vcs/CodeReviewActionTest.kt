package cc.unitmesh.devti.actions.vcs;

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class CodeReviewActionTest {

    @Test
    fun `given a valid GitHub URL, when checking if it matches the regex, then it should return true`() {
        val url = "https://github.com/username/repository"
        val result = url.matches(githubUrlRegex)
        assertTrue(result)
    }

    @Test
    fun `given an invalid GitHub URL, when checking if it matches the regex, then it should return false`() {
        val url = "https://example.com"
        val result = url.matches(githubUrlRegex)
        assertFalse(result)
    }

    @Test
    fun `given a GitHub URL with additional path, when checking if it matches the regex, then it should return true`() {
        val url = "https://github.com/username/repository/tree/main"
        val result = url.matches(githubUrlRegex)
        assertTrue(result)
    }

    @Test
    fun `given a GitHub URL without www, when checking if it matches the regex, then it should return true`() {
        val url = "https://github.com/username/repository"
        val result = url.matches(githubUrlRegex)
        assertTrue(result)
    }

    @Test
    fun `given a GitHub URL without http or https, when checking if it matches the regex, then it should return true`() {
        val url = "git://github.com/username/repository"
        val result = url.matches(githubUrlRegex)
        assertTrue(result)
    }

    @Test
    fun `given an empty string, when checking if it matches the regex, then it should return false`() {
        val url = ""
        val result = url.matches(githubUrlRegex)
        assertFalse(result)
    }
}