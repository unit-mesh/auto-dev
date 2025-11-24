package cc.unitmesh.devins.ui.integration

import cc.unitmesh.agent.tool.impl.SmartEditParams
import cc.unitmesh.agent.tool.impl.SmartEditTool
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.filesystem.FileInfo
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlinx.datetime.Clock

class SmartEditIntegrationTest {

    class RealToolFileSystem(private val rootDir: File) : ToolFileSystem {
        override fun getProjectPath(): String = rootDir.absolutePath

        override suspend fun readFile(path: String): String? {
            val file = File(path)
            return if (file.exists()) file.readText() else null
        }

        override suspend fun writeFile(path: String, content: String, createDirectories: Boolean) {
            val file = File(path)
            if (createDirectories) {
                file.parentFile?.mkdirs()
            }
            file.writeText(content)
        }

        override fun exists(path: String): Boolean = File(path).exists()

        override fun listFiles(path: String, pattern: String?): List<String> {
            return File(path).listFiles()?.map { it.absolutePath } ?: emptyList()
        }

        override fun resolvePath(relativePath: String): String {
            return if (File(relativePath).isAbsolute) relativePath else File(rootDir, relativePath).absolutePath
        }

        override fun getFileInfo(path: String): FileInfo? {
            val file = File(path)
            if (!file.exists()) return null
            return FileInfo(
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = file.length(),
                lastModified = file.lastModified()
            )
        }

        override fun createDirectory(path: String, createParents: Boolean) {
            val file = File(path)
            if (createParents) file.mkdirs() else file.mkdir()
        }

        override fun delete(path: String, recursive: Boolean) {
            File(path).deleteRecursively()
        }
    }

    @Test
    fun testSmartEditWithLLM() = runBlocking {
        if (!ConfigManager.exists()) {
            println("Skipping integration test: Config file not found.")
            return@runBlocking
        }

        val configWrapper = ConfigManager.load()
        val activeConfig = configWrapper.getActiveConfig()
        if (activeConfig == null) {
             println("Skipping integration test: No active config found.")
             return@runBlocking
        }
        val modelConfig = activeConfig.toModelConfig()
        
        // Skip if no API key (unless it's Ollama which might not need one, but let's assume we need a valid config)
        if (modelConfig.apiKey.isBlank() && modelConfig.provider != cc.unitmesh.llm.LLMProviderType.OLLAMA) {
             println("Skipping integration test: No API key found.")
             return@runBlocking
        }

        val llmService = KoogLLMService(modelConfig)
        
        // Create a temp file
        val tempFile = File.createTempFile("SmartEditTest", ".kt")
        tempFile.writeText("""
            fun hello() {
                println("Hello, World!")
            }
        """.trimIndent())
        tempFile.deleteOnExit()
        
        val filePath = tempFile.absolutePath
        val fileSystem = RealToolFileSystem(tempFile.parentFile)
        
        val tool = SmartEditTool(fileSystem, llmService)
        
        val prompt = """
            I have a file at '$filePath' with the following content:
            ```kotlin
            fun hello() {
                println("Hello, World!")
            }
            ```
            
            I want to change "Hello, World!" to "Hello, Integration Test!".
            
            Please generate the JSON for SmartEditParams to perform this change.
            The JSON should have keys: "filePath", "oldString", "newString", "instruction".
            Return ONLY the JSON string, no markdown formatting.
        """.trimIndent()
        
        val response = llmService.sendPrompt(prompt)
        println("LLM Response: $response")
        
        // Clean up response (remove markdown code blocks if present)
        val jsonString = response.replace("```json", "").replace("```", "").trim()
        
        try {
            val params = Json { ignoreUnknownKeys = true }.decodeFromString<SmartEditParams>(jsonString)
            
            // Ensure filePath is correct (LLM might hallucinate or use relative)
            val correctParams = params.copy(filePath = filePath)
            
            val invocation = tool.createInvocation(correctParams)
            val result = invocation.execute()
            
            assertTrue(result.isSuccess(), "Tool execution failed: ${result.getError()}")
            
            val newContent = tempFile.readText()
            assertEquals("""
                fun hello() {
                    println("Hello, Integration Test!")
                }
            """.trimIndent(), newContent)
            
        } catch (e: Exception) {
            println("Failed to parse JSON or execute tool: ${e.message}")
            // Fail the test if JSON parsing fails, unless it's an API error
            if (!response.contains("Error")) {
                throw e
            }
        }
    }

    @Test
    fun testMethodOnlyReplacement() = runBlocking {
        if (!ConfigManager.exists()) {
            println("Skipping integration test: Config file not found.")
            return@runBlocking
        }

        val configWrapper = ConfigManager.load()
        val activeConfig = configWrapper.getActiveConfig()
        if (activeConfig == null) {
             println("Skipping integration test: No active config found.")
             return@runBlocking
        }
        val modelConfig = activeConfig.toModelConfig()
        
        if (modelConfig.apiKey.isBlank() && modelConfig.provider != cc.unitmesh.llm.LLMProviderType.OLLAMA) {
             println("Skipping integration test: No API key found.")
             return@runBlocking
        }

        val llmService = KoogLLMService(modelConfig)
        
        val tempFile = File.createTempFile("SmartEditMethodTest", ".kt")
        val originalCode = """
            class Calculator {
                fun add(a: Int, b: Int): Int {
                    return a + b
                }
                
                fun subtract(a: Int, b: Int): Int {
                    return a - b
                }
            }
        """.trimIndent()
        
        tempFile.writeText(originalCode)
        tempFile.deleteOnExit()
        
        val filePath = tempFile.absolutePath
        val fileSystem = RealToolFileSystem(tempFile.parentFile)
        
        val tool = SmartEditTool(fileSystem, llmService)
        
        val prompt = """
            I have a Kotlin file with a Calculator class.
            I want to add logging to the add method.
            
            Current add method:
            ```kotlin
            fun add(a: Int, b: Int): Int {
                return a + b
            }
            ```
            
            Please generate the JSON for SmartEditParams to:
            1. Find the current add method
            2. Replace it with a version that logs the operation
            
            The JSON should have keys: "filePath", "oldString", "newString", "instruction".
            Use '$filePath' as the filePath.
            Return ONLY the JSON string, no markdown formatting.
        """.trimIndent()
        
        try {
            val response = llmService.sendPrompt(prompt)
            println("LLM Response: $response")
            
            val jsonString = response.replace("```json", "").replace("```", "").trim()
            val params = Json { ignoreUnknownKeys = true }.decodeFromString<SmartEditParams>(jsonString)
            val correctParams = params.copy(filePath = filePath)
            
            val invocation = tool.createInvocation(correctParams)
            val result = invocation.execute()
            
            assertTrue(result.isSuccess(), "Method replacement failed: ${result.getError()}")
            
            val newContent = tempFile.readText()
            assertTrue(newContent.contains("println") || newContent.contains("log"), 
                "New code should contain logging")
            
        } catch (e: Exception) {
            println("Method replacement test failed: ${e.message}")
            if (!e.message?.contains("Error")!!) {
                throw e
            }
        }
    }

    @Test
    fun testComplexIndentationPreservation() = runBlocking {
        if (!ConfigManager.exists()) {
            println("Skipping integration test: Config file not found.")
            return@runBlocking
        }

        val configWrapper = ConfigManager.load()
        val activeConfig = configWrapper.getActiveConfig()
        if (activeConfig == null) {
             println("Skipping integration test: No active config found.")
             return@runBlocking
        }
        val modelConfig = activeConfig.toModelConfig()
        
        if (modelConfig.apiKey.isBlank() && modelConfig.provider != cc.unitmesh.llm.LLMProviderType.OLLAMA) {
             println("Skipping integration test: No API key found.")
             return@runBlocking
        }

        val llmService = KoogLLMService(modelConfig)
        
        val tempFile = File.createTempFile("SmartEditIndentTest", ".kt")
        val originalCode = """
            class Outer {
                inner class Inner {
                    fun process() {
                        val result = 42
                    }
                }
            }
        """.trimIndent()
        
        tempFile.writeText(originalCode)
        tempFile.deleteOnExit()
        
        val filePath = tempFile.absolutePath
        val fileSystem = RealToolFileSystem(tempFile.parentFile)
        
        val tool = SmartEditTool(fileSystem, llmService)
        
        // Direct invocation without LLM to test indentation preservation
        val params = SmartEditParams(
            filePath = filePath,
            oldString = "val result = 42",
            newString = "val result = 100",
            instruction = "Update result value"
        )
        
        val invocation = tool.createInvocation(params)
        val result = invocation.execute()
        
        assertTrue(result.isSuccess())
        
        val newContent = tempFile.readText()
        // Verify indentation is preserved (12 spaces for the deeply nested line)
        val lines = newContent.lines()
        val resultLine = lines.find { it.contains("val result") }
        assertTrue(resultLine?.startsWith("            ") == true, 
            "Indentation should be preserved")
    }
}
