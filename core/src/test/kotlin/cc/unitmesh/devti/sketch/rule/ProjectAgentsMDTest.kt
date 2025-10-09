package cc.unitmesh.devti.sketch.rule

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.guessProjectDir
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectAgentsMDTest : BasePlatformTestCase() {

    @Test
    fun `should return null when AGENTS_md does not exist`() {
        val projectAgentsMD = ProjectAgentsMD(project)
        
        assertFalse(projectAgentsMD.hasAgentsMD())
        assertNull(projectAgentsMD.getAgentsMDContent())
        assertNull(projectAgentsMD.getRawContent())
    }

    @Test
    fun `should read AGENTS_md content when file exists`() {
        // Create AGENTS.md file in project root
        val projectDir = project.guessProjectDir()!!
        val agentsMDContent = """
            # Project Guidelines
            
            This is a test project for AGENTS.md support.
            
            ## Coding Standards
            - Use Kotlin for all new code
            - Follow the existing code style
        """.trimIndent()
        
        runWriteAction {
            val agentsMDFile = projectDir.createChildData(this, "AGENTS.md")
            agentsMDFile.setBinaryContent(agentsMDContent.toByteArray())
        }
        
        val projectAgentsMD = ProjectAgentsMD(project)
        
        assertTrue(projectAgentsMD.hasAgentsMD())
        assertNotNull(projectAgentsMD.getRawContent())
        assertEquals(agentsMDContent, projectAgentsMD.getRawContent())
        
        val wrappedContent = projectAgentsMD.getAgentsMDContent()
        assertNotNull(wrappedContent)
        assertTrue(wrappedContent!!.startsWith("<agents-md>"))
        assertTrue(wrappedContent.endsWith("</agents-md>"))
        assertTrue(wrappedContent.contains(agentsMDContent))
    }

    @Test
    fun `should read lowercase agents_md file`() {
        // Create agents.md file in project root
        val projectDir = project.guessProjectDir()!!
        val agentsMDContent = "# Lowercase AGENTS.md"
        
        runWriteAction {
            val agentsMDFile = projectDir.createChildData(this, "agents.md")
            agentsMDFile.setBinaryContent(agentsMDContent.toByteArray())
        }
        
        val projectAgentsMD = ProjectAgentsMD(project)
        
        assertTrue(projectAgentsMD.hasAgentsMD())
        assertEquals(agentsMDContent, projectAgentsMD.getRawContent())
    }

    @Test
    fun `should return null for empty AGENTS_md file`() {
        // Create empty AGENTS.md file
        val projectDir = project.guessProjectDir()!!
        
        runWriteAction {
            val agentsMDFile = projectDir.createChildData(this, "AGENTS.md")
            agentsMDFile.setBinaryContent("".toByteArray())
        }
        
        val projectAgentsMD = ProjectAgentsMD(project)
        
        assertTrue(projectAgentsMD.hasAgentsMD())
        assertNull(projectAgentsMD.getAgentsMDContent())
        assertNull(projectAgentsMD.getRawContent())
    }

    @Test
    fun `should return null for whitespace-only AGENTS_md file`() {
        // Create whitespace-only AGENTS.md file
        val projectDir = project.guessProjectDir()!!
        
        runWriteAction {
            val agentsMDFile = projectDir.createChildData(this, "AGENTS.md")
            agentsMDFile.setBinaryContent("   \n\n  \t  ".toByteArray())
        }
        
        val projectAgentsMD = ProjectAgentsMD(project)
        
        assertTrue(projectAgentsMD.hasAgentsMD())
        assertNull(projectAgentsMD.getAgentsMDContent())
        assertNull(projectAgentsMD.getRawContent())
    }
}

