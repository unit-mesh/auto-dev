package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemgrepLinterTest {
    @Test
    fun `should parse semgrep text output correctly`() {
        val output = """
┌─────────────────┐
│ 2 Code Findings │
└─────────────────┘
               
    insecure.py
    ❯❱ python.lang.security.deserialization.pickle.avoid-pickle
          Avoid using `pickle`, which is known to lead to code       
          execution vulnerabilities. When unpickling, the serialized 
          data could be manipulated to run arbitrary code. Instead,  
          consider serializing the relevant data as JSON or a similar
          text-based serialization format.                           
          Details: https://sg.run/OPwB                               
                                                                     
           11┆ return pickle.loads(data)  # Security issue
   
    ❯❱ python.lang.security.audit.eval-detected.eval-detected
          Detected the use of eval(). eval() can be dangerous if used
          to evaluate dynamic content. If this content can be input  
          from outside the program, this may be a code injection     
          vulnerability. Ensure evaluated content is not definable by
          external sources.                                          
          Details: https://sg.run/ZvrD                               
                                                                     
           32┆ eval(code)  # Dangerous
        """.trimIndent()

        val filePath = "insecure.py"
        val issues = SemgrepLinter.parseSemgrepOutput(output, filePath)

        assertEquals(2, issues.size, "Should parse 2 issues")

        // Check pickle issue
        val pickleIssue = issues[0]
        assertEquals(11, pickleIssue.line)
        assertEquals("python.lang.security.deserialization.pickle.avoid-pickle", pickleIssue.rule)
        assertEquals(LintSeverity.WARNING, pickleIssue.severity)
        assertTrue(pickleIssue.message.contains("pickle") || pickleIssue.message.contains("execution"))

        // Check eval issue
        val evalIssue = issues[1]
        assertEquals(32, evalIssue.line)
        assertEquals("python.lang.security.audit.eval-detected.eval-detected", evalIssue.rule)
        assertEquals(LintSeverity.WARNING, evalIssue.severity)
        assertTrue(evalIssue.message.contains("eval") || evalIssue.message.contains("injection"))
    }

    @Test
    fun `should handle empty output`() {
        val output = ""
        val issues = SemgrepLinter.parseSemgrepOutput(output, "test.py")
        assertEquals(0, issues.size)
    }

    @Test
    fun `should handle clean file with no findings`() {
        val output = """
Ran 291 rules on 1 file: 0 findings.
        """.trimIndent()
        val issues = SemgrepLinter.parseSemgrepOutput(output, "clean.py")
        assertEquals(0, issues.size)
    }
}

