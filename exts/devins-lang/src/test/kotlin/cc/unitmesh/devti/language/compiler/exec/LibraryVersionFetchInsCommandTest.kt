package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito

class LibraryVersionFetchInsCommandTest : BasePlatformTestCase() {

    fun testCommandName() {
        val project = Mockito.mock(Project::class.java)
        val command = LibraryVersionFetchInsCommand(project, "npm:react")
        assertEquals(BuiltinCommand.LIBRARY_VERSION_FETCH, command.commandName)
    }

    fun testParsePropWithType() = runBlocking {
        val project = Mockito.mock(Project::class.java)
        
        // Test with explicit type
        val command1 = LibraryVersionFetchInsCommand(project, "npm:react")
        val result1 = command1.execute()
        assertNotNull(result1)
        assertFalse("Should not be an error", result1!!.startsWith("DevInsError"))
        
        // Test Maven format
        val command2 = LibraryVersionFetchInsCommand(project, "maven:org.springframework:spring-core")
        val result2 = command2.execute()
        assertNotNull(result2)
        assertFalse("Should not be an error", result2!!.startsWith("DevInsError"))
    }

    fun testInvalidInput() = runBlocking {
        val project = Mockito.mock(Project::class.java)
        
        // Empty input
        val command1 = LibraryVersionFetchInsCommand(project, "")
        val result1 = command1.execute()
        assertNotNull(result1)
        assertTrue("Should show usage", result1!!.contains("Usage"))
        
        // Unsupported type
        val command2 = LibraryVersionFetchInsCommand(project, "unknown:package")
        val result2 = command2.execute()
        assertNotNull(result2)
        assertTrue("Should show unsupported error", result2!!.contains("Unsupported"))
    }

    fun testAutoDetection() = runBlocking {
        val project = myFixture.project
        
        // Test auto detection - this will try multiple registries
        val command = LibraryVersionFetchInsCommand(project, "react")
        val result = command.execute()
        assertNotNull(result)
        // Should either find versions or show no versions found
        assertTrue("Should either show versions or not found error", 
            result!!.contains("Library versions") || result.contains("No versions found"))
    }

    fun testNetworkErrorHandling() = runBlocking {
        val project = Mockito.mock(Project::class.java)
        
        // Test with invalid package name that should cause network error
        val command = LibraryVersionFetchInsCommand(project, "npm:this-package-definitely-does-not-exist-12345")
        val result = command.execute()
        assertNotNull(result)
        // Should gracefully handle network errors
        assertTrue("Should handle network errors gracefully", 
            result!!.contains("not found") || result.contains("failed"))
    }
}