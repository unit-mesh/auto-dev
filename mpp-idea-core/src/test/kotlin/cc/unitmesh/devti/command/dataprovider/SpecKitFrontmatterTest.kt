package cc.unitmesh.devti.command.dataprovider

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SpecKitFrontmatterTest : BasePlatformTestCase() {

    fun testParseSimpleFrontmatter() {
        val markdown = """
            ---
            description: Test description
            ---

            # Content
            This is the content.
        """.trimIndent()

        val (frontmatter, content) = SkillFrontmatter.parse(markdown)

        assertNotNull(frontmatter)
        assertEquals("Test description", frontmatter!!.description)
        assertTrue(content.contains("# Content"))
        assertTrue(content.contains("This is the content."))
    }
    
    fun testParseFrontmatterWithVariables() {
        val markdown = """
            ---
            description: Execute the implementation planning workflow
            variables:
              FEATURE_SPEC: "path/to/spec.md"
              IMPL_PLAN: "path/to/plan.md"
            ---

            ## User Input

            ```text
            ${'$'}ARGUMENTS
            ```
        """.trimIndent()

        val (frontmatter, content) = SkillFrontmatter.parse(markdown)

        assertNotNull(frontmatter)
        assertEquals("Execute the implementation planning workflow", frontmatter!!.description)
        assertEquals(2, frontmatter.variables.size)
        assertEquals("path/to/spec.md", frontmatter.variables["FEATURE_SPEC"])
        assertEquals("path/to/plan.md", frontmatter.variables["IMPL_PLAN"])
        assertTrue(content.contains("## User Input"))
    }

    fun testParseNoFrontmatter() {
        val markdown = """
            # Just Content
            No frontmatter here.
        """.trimIndent()

        val (frontmatter, content) = SkillFrontmatter.parse(markdown)

        assertNull(frontmatter)
        assertEquals(markdown, content)
    }

    fun testParseFrontmatterWithAdditionalFields() {
        val markdown = """
            ---
            description: Test
            author: John Doe
            version: 1.0
            ---

            Content
        """.trimIndent()

        val (frontmatter, content) = SkillFrontmatter.parse(markdown)

        assertNotNull(frontmatter)
        assertEquals("Test", frontmatter!!.description)
        assertEquals(2, frontmatter.additionalFields.size)
        assertEquals("John Doe", frontmatter.additionalFields["author"])
        assertEquals("1.0", frontmatter.additionalFields["version"].toString())
    }

    fun testParseFrontmatterWithName() {
        val markdown = """
            ---
            name: PDF Skill
            description: Handle PDF operations
            ---

            Content
        """.trimIndent()

        val (frontmatter, content) = SkillFrontmatter.parse(markdown)

        assertNotNull(frontmatter)
        assertEquals("PDF Skill", frontmatter!!.name)
        assertEquals("Handle PDF operations", frontmatter.description)
    }
    
    fun testParseInvalidYaml() {
        val markdown = """
            ---
            invalid: yaml: content: here
            ---
            
            Content
        """.trimIndent()
        
        val (frontmatter, content) = SpecKitFrontmatter.parse(markdown)
        
        // Should handle invalid YAML gracefully
        assertTrue(content.contains("Content"))
    }
}

