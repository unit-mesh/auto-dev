package cc.unitmesh.devins.compiler.service

import cc.unitmesh.devins.filesystem.EmptyFileSystem
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DevInsCompilerServiceTest {

    @AfterTest
    fun tearDown() {
        DevInsCompilerService.reset()
    }

    @Test
    fun `getInstance returns DefaultDevInsCompilerService when not set`() {
        val service = DevInsCompilerService.getInstance()
        assertNotNull(service)
        assertEquals("DefaultDevInsCompilerService (mpp-core)", service.getName())
        assertFalse(service.supportsIdeFeatures())
    }
    
    @Test
    fun `setInstance allows custom implementation`() {
        val customService = object : DevInsCompilerService {
            override suspend fun compile(source: String, fileSystem: cc.unitmesh.devins.filesystem.ProjectFileSystem) =
                cc.unitmesh.devins.compiler.result.DevInsCompiledResult(output = "custom: $source")
            
            override suspend fun compile(
                source: String,
                fileSystem: cc.unitmesh.devins.filesystem.ProjectFileSystem,
                variables: Map<String, Any>
            ) = compile(source, fileSystem)
            
            override fun supportsIdeFeatures() = true
            override fun getName() = "CustomCompilerService"
        }
        
        DevInsCompilerService.setInstance(customService)
        
        val service = DevInsCompilerService.getInstance()
        assertEquals("CustomCompilerService", service.getName())
        assertTrue(service.supportsIdeFeatures())
    }
    
    @Test
    fun `reset restores default implementation`() {
        val customService = object : DevInsCompilerService {
            override suspend fun compile(source: String, fileSystem: cc.unitmesh.devins.filesystem.ProjectFileSystem) =
                cc.unitmesh.devins.compiler.result.DevInsCompiledResult(output = "custom")
            
            override suspend fun compile(
                source: String,
                fileSystem: cc.unitmesh.devins.filesystem.ProjectFileSystem,
                variables: Map<String, Any>
            ) = compile(source, fileSystem)
            
            override fun supportsIdeFeatures() = true
            override fun getName() = "CustomCompilerService"
        }
        
        DevInsCompilerService.setInstance(customService)
        assertEquals("CustomCompilerService", DevInsCompilerService.getInstance().getName())
        
        DevInsCompilerService.reset()
        assertEquals("DefaultDevInsCompilerService (mpp-core)", DevInsCompilerService.getInstance().getName())
    }
}

class DefaultDevInsCompilerServiceTest {

    @Test
    fun `compile returns output for simple text`() = runTest {
        val service = DefaultDevInsCompilerService()
        val result = service.compile("Hello World", EmptyFileSystem())

        assertEquals("Hello World", result.output)
        assertFalse(result.hasError)
    }

    @Test
    fun `compile handles DevIns commands`() = runTest {
        val service = DefaultDevInsCompilerService()
        // /file command should produce placeholder in mpp-core implementation
        val result = service.compile("/file:test.kt", EmptyFileSystem())

        // The mpp-core compiler outputs placeholders for commands
        assertNotNull(result.output)
    }

    @Test
    fun `supportsIdeFeatures returns false`() {
        val service = DefaultDevInsCompilerService()
        assertFalse(service.supportsIdeFeatures())
    }

    @Test
    fun `getName returns correct name`() {
        val service = DefaultDevInsCompilerService()
        assertEquals("DefaultDevInsCompilerService (mpp-core)", service.getName())
    }

    @Test
    fun `compile with variables works`() = runTest {
        val service = DefaultDevInsCompilerService()
        val variables = mapOf(
            "name" to "test",
            "count" to 42,
            "enabled" to true
        )

        val result = service.compile("Hello \$name", EmptyFileSystem(), variables)
        assertNotNull(result.output)
    }
}

