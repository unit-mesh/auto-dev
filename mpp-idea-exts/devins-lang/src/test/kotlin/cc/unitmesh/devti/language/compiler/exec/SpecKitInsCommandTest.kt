package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.language.compiler.exec.speckit.SpecKitInsCommand
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class SpecKitInsCommandTest : BasePlatformTestCase() {

    private lateinit var promptsDir: Path

    override fun setUp() {
        super.setUp()
        
        // Create .github/prompts directory in test project
        val projectPath = project.basePath ?: throw IllegalStateException("Project path is null")
        promptsDir = Path.of(projectPath, ".github", "prompts")
        promptsDir.createDirectories()
        
        // Create sample prompt files
        createSamplePromptFile("clarify", """
            ---
            description: Clarify underspecified areas in the feature spec
            ---
            
            ## User Input
            
            ```text
            ${'$'}ARGUMENTS
            ```
            
            You **MUST** consider the user input before proceeding.
            
            ## Outline
            
            Clarify the following aspects:
            1. Functional requirements
            2. User scenarios
            3. Edge cases
        """.trimIndent())
        
        createSamplePromptFile("specify", """
            ---
            description: Create feature specification from natural language
            ---
            
            ## User Input
            
            ```text
            ${'$'}ARGUMENTS
            ```
            
            You **MUST** consider the user input before proceeding.
            
            ## Outline
            
            Create a specification for the feature described above.
        """.trimIndent())
        
        createSamplePromptFile("plan", """
            ---
            description: Create technical implementation plan
            ---
            
            ## User Input
            
            ```text
            ${'$'}ARGUMENTS
            ```
            
            Create a technical plan based on the tech stack provided.
        """.trimIndent())
    }

    override fun tearDown() {
        try {
            // Clean up test files
            if (::promptsDir.isInitialized && Files.exists(promptsDir)) {
                promptsDir.toFile().deleteRecursively()
            }
        } finally {
            super.tearDown()
        }
    }

    private fun createSamplePromptFile(subcommand: String, content: String) {
        val promptFile = promptsDir.resolve("speckit.$subcommand.prompt.md")
        promptFile.writeText(content)
    }

    fun `test isApplicable returns true when prompts directory exists`() {
        val command = SpecKitInsCommand(project, "clarify", "test arguments")
        assertTrue(command.isApplicable())
    }

    fun `test isApplicable returns false when prompts directory does not exist`() {
        // Delete the prompts directory
        promptsDir.toFile().deleteRecursively()
        
        val command = SpecKitInsCommand(project, "clarify", "test arguments")
        assertFalse(command.isApplicable())
    }

    fun `test execute with clarify subcommand`() = runBlocking {
        val arguments = "What are the edge cases for user authentication?"
        val command = SpecKitInsCommand(project, "clarify", arguments)
        
        val result = command.execute()
        
        assertNotNull(result)
        assertTrue(result!!.contains("Clarify the following aspects"))
        assertTrue(result.contains(arguments))
        assertFalse(result.contains("\$ARGUMENTS"))
    }

    fun `test execute with specify subcommand`() = runBlocking {
        val arguments = "Build a photo organization application"
        val command = SpecKitInsCommand(project, "specify", arguments)
        
        val result = command.execute()
        
        assertNotNull(result)
        assertTrue(result!!.contains("Create a specification"))
        assertTrue(result.contains(arguments))
        assertFalse(result.contains("\$ARGUMENTS"))
    }

    fun `test execute with plan subcommand`() = runBlocking {
        val arguments = "Use React with TypeScript and PostgreSQL"
        val command = SpecKitInsCommand(project, "plan", arguments)
        
        val result = command.execute()
        
        assertNotNull(result)
        assertTrue(result!!.contains("Create a technical plan"))
        assertTrue(result.contains(arguments))
    }

    fun `test execute with speckit dot prefix`() = runBlocking {
        val arguments = "Test arguments"
        val command = SpecKitInsCommand(project, "speckit.clarify", arguments)
        
        val result = command.execute()
        
        assertNotNull(result)
        assertTrue(result!!.contains("Clarify the following aspects"))
    }

    fun `test execute with dot prefix only`() = runBlocking {
        val arguments = "Test arguments"
        val command = SpecKitInsCommand(project, ".clarify", arguments)
        
        val result = command.execute()
        
        assertNotNull(result)
        assertTrue(result!!.contains("Clarify the following aspects"))
    }

    fun `test execute with non-existent subcommand`() = runBlocking {
        val command = SpecKitInsCommand(project, "nonexistent", "test")

        val result = command.execute()

        assertNotNull(result)
        assertTrue(result!!.contains("<DevInsError>"))
        assertTrue(result.contains("Prompt file not found"))
    }

    fun `test execute with empty prop`() = runBlocking {
        val command = SpecKitInsCommand(project, "", "test")

        val result = command.execute()

        assertNotNull(result)
        assertTrue(result!!.contains("<DevInsError>"))
        assertTrue(result.contains("Invalid speckit command format"))
    }

    fun `test arguments replacement in prompt template`() = runBlocking {
        val testArguments = "This is a test argument with special characters: @#$%"
        val command = SpecKitInsCommand(project, "clarify", testArguments)
        
        val result = command.execute()
        
        assertNotNull(result)
        assertTrue(result!!.contains(testArguments))
        assertFalse(result.contains("\$ARGUMENTS"))
    }
}

