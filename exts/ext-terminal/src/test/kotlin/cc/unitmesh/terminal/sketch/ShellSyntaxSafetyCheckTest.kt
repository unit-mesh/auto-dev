package cc.unitmesh.terminal.sketch

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ShellSyntaxSafetyCheckTest : BasePlatformTestCase() {
    fun testCheckDangerousCommandByPsi() {
        val project = project // Use the test fixture's project

        val safeCommands = listOf(
            "ls -la",
            "cd /home/user",
            "echo 'Hello World'"
        )

        for (command in safeCommands) {
            val result = ShellSyntaxSafetyCheck.checkDangerousCommand(project, command)
            assertFalse("Should be safe: $command", result.first)
            assertEquals("", result.second)
        }

        val dangerousCommands = mapOf(
            "rm -rf /tmp" to "Dangerous rm command detected",
            "sudo rm file.txt" to "Removing files with elevated privileges",
            "mkfs /dev/sda1" to "Filesystem formatting command",
            "dd if=/dev/zero of=/dev/sda" to "Low-level disk operation",
            "chmod -R 777 /var" to "Recursive chmod with insecure permissions (e.g., 777)",
            "rm /" to "Operation targeting root directory"
        )

        for ((command, expectedMessage) in dangerousCommands) {
            val result = ShellSyntaxSafetyCheck.checkDangerousCommand(project, command)
            assertTrue("Should be dangerous: $command", result.first)
            assertEquals(expectedMessage, result.second)
        }

        val forkBomb = ":(){ :|:& };:"
        val result = ShellSyntaxSafetyCheck.checkDangerousCommand(project, forkBomb)
        assertTrue("Fork bomb should be detected", result.first)
        assertEquals("Potential fork bomb", result.second)
    }

    fun testCheckDangerousCommandByPsiWithForce() {
        val result = ShellSyntaxSafetyCheck.checkDangerousCommand(project, "rm -f /tmp")
        assertTrue("Should be dangerous: rm -f /tmp", result.first)
        assertEquals("Dangerous rm command detected", result.second)
    }
}
