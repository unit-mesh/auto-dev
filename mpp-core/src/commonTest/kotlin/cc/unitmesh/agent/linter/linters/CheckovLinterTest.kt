package cc.unitmesh.agent.linter.linters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckovLinterTest {
    @Test
    fun `should parse checkov output correctly`() {
        val output = """
Check: CKV_AWS_260: "Ensure no security groups allow ingress from 0.0.0.0:0 to port 80"
	FAILED for resource: aws_security_group.example
	File: /bad_terraform.tf:8-19
Check: CKV_AWS_24: "Ensure no security groups allow ingress from 0.0.0.0:0 to port 22"
	FAILED for resource: aws_security_group.example
	File: /bad_terraform.tf:8-19
Check: CKV_AWS_93: "Ensure S3 bucket policy does not lockout all but root user"
	PASSED for resource: aws_s3_bucket.example
	File: /bad_terraform.tf:3-6
Check: CKV_AWS_79: "Ensure Instance Metadata Service Version 1 is not enabled"
	FAILED for resource: aws_instance.example
	File: /bad_terraform.tf:21-25
Check: CKV_AWS_18: "Ensure the S3 bucket has access logging enabled"
	FAILED for resource: aws_s3_bucket.example
	File: /bad_terraform.tf:3-6
        """.trimIndent()

        val filePath = "bad_terraform.tf"
        val issues = CheckovLinter.parseCheckovOutput(output, filePath)

        assertEquals(4, issues.size, "Should parse 4 failed checks")

        // Check first issue
        val firstIssue = issues[0]
        assertEquals(8, firstIssue.line)
        assertEquals("CKV_AWS_260", firstIssue.rule)
        assertTrue(firstIssue.message.contains("security groups"))

        // Check instance issue
        val instanceIssue = issues.find { it.rule == "CKV_AWS_79" }
        assertEquals(21, instanceIssue?.line)
        assertTrue(instanceIssue?.message?.contains("Instance Metadata") == true)

        // Check S3 issue
        val s3Issue = issues.find { it.rule == "CKV_AWS_18" }
        assertEquals(3, s3Issue?.line)
        assertTrue(s3Issue?.message?.contains("access logging") == true)
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = CheckovLinter.parseCheckovOutput(output, "test.tf")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should only report failed checks`() {
        val output = """
Check: CKV_AWS_93: "Ensure S3 bucket policy does not lockout all but root user"
	PASSED for resource: aws_s3_bucket.example
	File: /bad_terraform.tf:3-6
        """.trimIndent()

        val issues = CheckovLinter.parseCheckovOutput(output, "test.tf")
        assertEquals(0, issues.size, "Should not report passed checks")
    }
}

