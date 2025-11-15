package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitleaksLinterTest {
    @Test
    fun `should parse gitleaks verbose output correctly`() {
        val output = """
    ○
    │╲
    │ ○
    ○ ░
    ░    gitleaks

Finding:     API_KEY = "sk-1234567890abcdef"
Secret:      sk-1234567890abcdef
RuleID:      generic-api-key
Entropy:     4.247928
File:        config.py
Line:        23
Fingerprint: config.py:generic-api-key:23

Finding:     SSH_KEY = "-----BEGIN OPENSSH PRIVATE KEY-----"
Secret:      -----BEGIN OPENSSH PRIVATE KEY-----
RuleID:      private-key
Entropy:     4.489192
File:        config.py
Line:        21
Fingerprint: config.py:private-key:21

Finding:     STRIPE_API_KEY = "sk_live_51H5z3k2eZvKYlo2C1234567890"
Secret:      sk_live_51H5z3k2eZvKYlo2C1234567890
RuleID:      stripe-access-token
Entropy:     4.457575
File:        config.py
Line:        18
Fingerprint: config.py:stripe-access-token:18

12:44PM INF scanned ~13639 bytes (13.64 KB) in 11.3ms
12:44PM WRN leaks found: 3
        """.trimIndent()

        val filePath = "config.py"
        val issues = GitleaksLinter.parseGitleaksOutput(output, filePath)

        assertEquals(3, issues.size, "Should parse 3 secrets")

        // Check first secret
        val apiKeyIssue = issues[0]
        assertEquals(23, apiKeyIssue.line)
        assertEquals("generic-api-key", apiKeyIssue.rule)
        assertEquals(LintSeverity.ERROR, apiKeyIssue.severity)
        assertTrue(apiKeyIssue.message.contains("Secret detected"))

        // Check second secret
        val privateKeyIssue = issues[1]
        assertEquals(21, privateKeyIssue.line)
        assertEquals("private-key", privateKeyIssue.rule)
        assertEquals(LintSeverity.ERROR, privateKeyIssue.severity)

        // Check third secret
        val stripeIssue = issues[2]
        assertEquals(18, stripeIssue.line)
        assertEquals("stripe-access-token", stripeIssue.rule)
        assertEquals(LintSeverity.ERROR, stripeIssue.severity)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = GitleaksLinter.parseGitleaksOutput(output, "test.py")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle clean file with no secrets`() {
        val output = """
    ○
    │╲
    │ ○
    ○ ░
    ░    gitleaks

12:44PM INF scanned ~499 bytes (499 bytes) in 11.3ms
12:44PM INF no leaks found
        """.trimIndent()
        val issues = GitleaksLinter.parseGitleaksOutput(output, "clean.py")
        assertEquals(0, issues.size)
    }
}

