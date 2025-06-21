package cc.unitmesh.devti.command

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EditRequestParserTest {
    private val parser = EditRequestParser()

    @Test
    fun `should parse valid YAML format`() {
        val content = """
            target_file: "src/main/kotlin/Example.kt"
            instructions: "Add a new method"
            code_edit: |
              class Example {
                  fun newMethod() {
                      println("Hello")
                  }
              }
        """.trimIndent()

        val result = parser.parse(content)
        
        assertNotNull(result)
        assertEquals("src/main/kotlin/Example.kt", result.targetFile)
        assertEquals("Add a new method", result.instructions)
        assertTrue(result.codeEdit.contains("class Example"))
        assertTrue(result.codeEdit.contains("fun newMethod()"))
    }

    @Test
    fun `should parse YAML format without instructions`() {
        val content = """
            target_file: "test.kt"
            code_edit: "fun test() {}"
        """.trimIndent()

        val result = parser.parse(content)
        
        assertNotNull(result)
        assertEquals("test.kt", result.targetFile)
        assertEquals("", result.instructions)
        assertEquals("fun test() {}", result.codeEdit)
    }

    @Test
    fun `should parse advanced format with block scalar`() {
        val content = """
            target_file: src/Example.kt
            instructions: Add method
            code_edit: |
              class Example {
                  fun method() {}
              }
        """.trimIndent()

        val result = parser.parse(content)
        
        assertNotNull(result)
        assertEquals("src/Example.kt", result.targetFile)
        assertEquals("Add method", result.instructions)
        assertTrue(result.codeEdit.contains("class Example"))
    }

    @Test
    fun `should parse advanced format with quoted string`() {
        val content = """
            target_file: "test.kt"
            instructions: "Test instruction"
            code_edit: "fun test() {\n    println(\"Hello\")\n}"
        """.trimIndent()

        val result = parser.parse(content)
        
        assertNotNull(result)
        assertEquals("test.kt", result.targetFile)
        assertEquals("Test instruction", result.instructions)
        assertTrue(result.codeEdit.contains("println(\"Hello\")"))
    }

    @Test
    fun `should parse legacy format`() {
        val content = """
            target_file = "legacy.kt"
            instructions = "Legacy instruction"
            code_edit = "class Legacy {}"
        """.trimIndent()

        val result = parser.parse(content)
        
        assertNotNull(result)
        assertEquals("legacy.kt", result.targetFile)
        assertEquals("Legacy instruction", result.instructions)
        assertEquals("class Legacy {}", result.codeEdit)
    }

    @Test
    fun `should handle escape sequences in code_edit`() {
        val content = """
            target_file: "test.kt"
            code_edit: "fun test() {\n    println(\"Hello \\\"World\\\"\")\n}"
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.codeEdit.contains("println(\"Hello \"World\"\")"))
    }

    @Test
    fun `should return null for empty content`() {
        val result = parser.parse("")
        assertNull(result)
    }

    @Test
    fun `should return null for blank content`() {
        val result = parser.parse("   \n  \t  ")
        assertNull(result)
    }

    @Test
    fun `should return null when no format matches`() {
        val content = "This is not a valid format at all"
        val result = parser.parse(content)
        assertNull(result)
    }

    @Test
    fun `should throw exception for YAML with missing target_file`() {
        val content = """
            instructions: "Test"
            code_edit: "test code"
        """.trimIndent()

        assertFailsWith<ParseException.MissingFieldException> {
            parser.parseAsYaml(content)
        }
    }

    @Test
    fun `should throw exception for YAML with missing code_edit`() {
        val content = """
            target_file: "test.kt"
            instructions: "Test"
        """.trimIndent()

        assertFailsWith<ParseException.MissingFieldException> {
            parser.parseAsYaml(content)
        }
    }

    @Test
    fun `should throw exception for invalid target_file with path traversal`() {
        val content = """
            target_file: "../../../etc/passwd"
            code_edit: "malicious code"
        """.trimIndent()

        assertFailsWith<ParseException.InvalidFieldException> {
            parser.parseAsYaml(content)
        }
    }

    @Test
    fun `should throw exception for blank target_file`() {
        val content = """
            target_file: ""
            code_edit: "test code"
        """.trimIndent()

        assertFailsWith<ParseException.InvalidFieldException> {
            parser.parseAsYaml(content)
        }
    }

    @Test
    fun `should throw exception for blank code_edit`() {
        val content = """
            target_file: "test.kt"
            code_edit: ""
        """.trimIndent()

        assertFailsWith<ParseException.InvalidFieldException> {
            parser.parseAsYaml(content)
        }
    }

    @Test
    fun `should handle complex code with nested braces and quotes`() {
        val content = """
            target_file: "complex.kt"
            code_edit: |
              class Complex {
                  fun method() {
                      val map = mapOf("key" to "value")
                      if (condition) {
                          println("Nested {braces} and \"quotes\"")
                      }
                  }
              }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.codeEdit.contains("mapOf(\"key\" to \"value\")"))
        assertTrue(result.codeEdit.contains("Nested {braces} and \"quotes\""))
    }

    @Test
    fun `should parse format with single quotes`() {
        val content = """
            target_file: 'single.kt'
            instructions: 'Single quote instruction'
            code_edit: 'fun test() { println("mixed quotes") }'
        """.trimIndent()

        val result = parser.parse(content)
        
        assertNotNull(result)
        assertEquals("single.kt", result.targetFile)
        assertEquals("Single quote instruction", result.instructions)
        assertTrue(result.codeEdit.contains("println(\"mixed quotes\")"))
    }

    @Test
    fun `should handle multiline instructions`() {
        val content = """
            target_file: "test.kt"
            instructions: |
              This is a multiline instruction
              that spans multiple lines
              with detailed explanation
            code_edit: "fun test() {}"
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.instructions.contains("multiline instruction"))
        assertTrue(result.instructions.contains("detailed explanation"))
    }

    @Test
    fun `should handle unclosed quotes in legacy format`() {
        val content = """
            target_file = "test.kt"
            code_edit = "unclosed quote content
        """.trimIndent()

        assertFailsWith<ParseException.QuoteParseException> {
            parser.parseAsLegacyFormat(content)
        }
    }

    @Test
    fun `should handle malformed YAML`() {
        val content = """
            target_file: test.kt
            code_edit: |
              invalid yaml structure
            - this is not valid
            malformed: [unclosed array
        """.trimIndent()

        assertFailsWith<ParseException.YamlParseException> {
            parser.parseAsYaml(content)
        }
    }

    @Test
    fun `should handle advanced format with missing colon`() {
        val content = """
            target_file "test.kt"
            code_edit "missing colon"
        """.trimIndent()

        assertFailsWith<ParseException.MissingFieldException> {
            parser.parseAsAdvancedFormat(content)
        }
    }

    @Test
    fun `should parse content with special characters in file path`() {
        val content = """
            target_file: "src/main/kotlin/com/example/Test-File_123.kt"
            code_edit: "class TestFile123 {}"
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("src/main/kotlin/com/example/Test-File_123.kt", result.targetFile)
    }

    @Test
    fun `should handle code with existing code markers`() {
        val content = """
            target_file: "test.kt"
            code_edit: |
              class Test {
                  fun newMethod() {}

                  // ... existing methods ...
              }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.codeEdit.contains("// ... existing methods ..."))
    }

    @Test
    fun `should validate and reject null YAML content`() {
        val content = "null"

        assertFailsWith<ParseException.YamlParseException> {
            parser.parseAsYaml(content)
        }
    }
}
