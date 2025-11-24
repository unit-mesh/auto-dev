package cc.unitmesh.server.cli

import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.document.DocumentAgent
import cc.unitmesh.agent.document.DocumentTask
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.devins.document.*
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.compression.TokenInfo
import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.io.File

/**
 * JVM CLI for testing DocumentAgent with PPTX, DOCX, PDF files
 * 
 * Usage:
 * ```bash
 * ./gradlew :mpp-server:runDocumentCli -PprojectPath=/path/to/docs -Pquery="What is this about?" [-PdocumentPath=specific.pptx]
 * ```
 */
object DocumentCli {
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("=".repeat(80))
        println("AutoDev Document CLI (JVM - Tika Support)")
        println("=".repeat(80))
        
        // Parse arguments
        val projectPath = System.getProperty("projectPath") ?: args.getOrNull(0) ?: run {
            System.err.println("Usage: -PprojectPath=<path> -Pquery=<query> [-PdocumentPath=<doc>]")
            return
        }
        val query = System.getProperty("query") ?: args.getOrNull(1) ?: run {
            System.err.println("Usage: -PprojectPath=<path> -Pquery=<query> [-PdocumentPath=<doc>]")
            return
        }
        val documentPath = System.getProperty("documentPath") ?: args.getOrNull(2)
        
        println("📂 Project Path: $projectPath")
        println("❓ Query: $query")
        if (documentPath != null) {
            println("📄 Document: $documentPath")
        }
        println()
        
        runBlocking {
            try {
                // Initialize platform parsers
                println("🔧 Initializing document parsers (Tika)...")
                DocumentRegistry.initializePlatformParsers()
                println("✅ Parsers initialized")
                println()
                
                // Scan and register documents
                val projectDir = File(projectPath).absoluteFile
                if (!projectDir.exists()) {
                    System.err.println("❌ Project path does not exist: $projectPath")
                    return@runBlocking
                }
                
                val startTime = System.currentTimeMillis()
                val documents = scanDocuments(projectDir)
                val scanTime = System.currentTimeMillis() - startTime
                
                println("📖 Found ${documents.size} documents (${scanTime}ms)")
                documents.take(10).forEach { doc ->
                    val relativePath = doc.relativeTo(projectDir).path
                    val size = doc.length() / 1024
                    println("   • $relativePath (${size}KB)")
                }
                if (documents.size > 10) {
                    println("   ... and ${documents.size - 10} more")
                }
                println()
                
                if (documents.isEmpty()) {
                    println("⚠️  No documents found in: $projectPath")
                    return@runBlocking
                }
                
                // Register documents
                println("📝 Registering documents...")
                var registeredCount = 0
                val registerStartTime = System.currentTimeMillis()
                
                for (doc in documents) {
                    val relativePath = doc.relativeTo(projectDir).path
                    val formatType = DocumentParserFactory.detectFormat(doc.name)
                    
                    if (registerDocument(doc, relativePath)) {
                        val typeSymbol = when (formatType) {
                            DocumentFormatType.MARKDOWN -> "📝"
                            DocumentFormatType.PDF -> "📕"
                            DocumentFormatType.DOCX -> "📘"
                            DocumentFormatType.PLAIN_TEXT -> "📄"
                            else -> "📄"
                        }
                        println("  $typeSymbol $relativePath")
                        registeredCount++
                    } else {
                        println("  ✗ $relativePath (no parser)")
                    }
                }
                
                val registerTime = System.currentTimeMillis() - registerStartTime
                println("✅ Registered $registeredCount/${documents.size} documents (${registerTime}ms)")
                println()
                
                // Show registered documents summary
                val registeredPaths = DocumentRegistry.getRegisteredPaths()
                println("📚 Document Registry Summary:")
                println("   Total registered: ${registeredPaths.size}")
                
                // Count by type
                val byType = registeredPaths.groupBy { path ->
                    val ext = File(path).extension.lowercase()
                    when (ext) {
                        "md" -> "Markdown"
                        "pdf" -> "PDF"
                        "docx", "doc" -> "Word"
                        "pptx", "ppt" -> "PowerPoint"
                        else -> "Other"
                    }
                }
                byType.forEach { (type, paths) ->
                    println("   - $type: ${paths.size}")
                }
                println()
                
                // Create DocumentAgent
                println("🧠 Creating DocumentAgent...")
                
                // Load configuration from ~/.autodev/config.yaml
                val configFile = File(System.getProperty("user.home"), ".autodev/config.yaml")
                if (!configFile.exists()) {
                    System.err.println("❌ Configuration file not found: ${configFile.absolutePath}")
                    System.err.println("   Please create ~/.autodev/config.yaml with your LLM configuration")
                    return@runBlocking
                }
                
                val yamlContent = configFile.readText()
                val yaml = Yaml(configuration = com.charleskorn.kaml.YamlConfiguration(strictMode = false))
                val config = yaml.decodeFromString(AutoDevConfig.serializer(), yamlContent)
                
                val activeName = config.active
                val activeConfig = config.configs.find { it.name == activeName }
                
                if (activeConfig == null) {
                    System.err.println("❌ Active configuration '$activeName' not found in config.yaml")
                    System.err.println("   Available configs: ${config.configs.map { it.name }.joinToString(", ")}")
                    return@runBlocking
                }
                
                println("📝 Using config: ${activeConfig.name} (${activeConfig.provider}/${activeConfig.model})")
                
                // Convert provider string to LLMProviderType
                val providerType = when (activeConfig.provider.lowercase()) {
                    "openai" -> LLMProviderType.OPENAI
                    "anthropic" -> LLMProviderType.ANTHROPIC
                    "google" -> LLMProviderType.GOOGLE
                    "deepseek" -> LLMProviderType.DEEPSEEK
                    "ollama" -> LLMProviderType.OLLAMA
                    "openrouter" -> LLMProviderType.OPENROUTER
                    "glm" -> LLMProviderType.GLM
                    "qwen" -> LLMProviderType.QWEN
                    "kimi" -> LLMProviderType.KIMI
                    else -> LLMProviderType.CUSTOM_OPENAI_BASE
                }
                
                val llmService = KoogLLMService(
                    ModelConfig(
                        provider = providerType,
                        modelName = activeConfig.model,
                        apiKey = activeConfig.apiKey,
                        temperature = activeConfig.temperature ?: 0.7,
                        maxTokens = activeConfig.maxTokens ?: 4096,
                        baseUrl = activeConfig.baseUrl ?: ""
                    )
                )
                
                val renderer = ConsoleRenderer()
                val mcpConfigService = McpToolConfigService(ToolConfigFile())
                val dummyParser = DocumentParserFactory.createParserForFile("dummy.md")!!
                
                val agent = DocumentAgent(
                    llmService = llmService,
                    parserService = dummyParser,
                    renderer = renderer,
                    fileSystem = null,
                    shellExecutor = null,
                    mcpToolConfigService = mcpConfigService,
                    enableLLMStreaming = true
                )
                
                println("✅ Agent created")
                println()
                
                // Execute query
                println("🔍 Executing query...")
                println()
                
                val queryStartTime = System.currentTimeMillis()
                val result = agent.execute(
                    DocumentTask(
                        query = query,
                        documentPath = documentPath
                    ),
                    onProgress = { }
                )
                
                val queryTime = System.currentTimeMillis() - queryStartTime
                
                println()
                println("=".repeat(80))
                println("📊 Result:")
                println("=".repeat(80))
                println(result.content)
                println()
                
                if (result.success) {
                    println("✅ Query completed successfully")
                    println("⏱️  Query time: ${queryTime}ms")
                    println("⏱️  Total time: ${System.currentTimeMillis() - startTime}ms")
                } else {
                    println("❌ Query failed")
                }
                
            } catch (e: Exception) {
                System.err.println("❌ Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Scan directory for documents
     */
    private fun scanDocuments(
        dir: File, 
        extensions: List<String> = listOf(".md", ".pdf", ".docx", ".pptx", ".txt")
    ): List<File> {
        val documents = mutableListOf<File>()
        val skipDirs = setOf("node_modules", ".git", "build", "dist", "target", ".gradle", "bin", ".idea")
        
        fun scanRecursive(current: File) {
            if (!current.canRead()) return
            
            if (current.isDirectory) {
                if (skipDirs.contains(current.name) || current.name.startsWith(".")) {
                    return
                }
                current.listFiles()?.forEach { scanRecursive(it) }
            } else if (current.isFile) {
                val ext = "." + current.extension.lowercase()
                if (extensions.contains(ext)) {
                    documents.add(current)
                }
            }
        }
        
        scanRecursive(dir)
        return documents
    }
    
    /**
     * Register a document with the DocumentRegistry
     */
    private suspend fun registerDocument(file: File, relativePath: String): Boolean {
        return try {
            // Get parser for this file type
            val parser = DocumentParserFactory.createParserForFile(file.name) ?: return false
            
            // Detect format
            val formatType = DocumentParserFactory.detectFormat(file.name) ?: DocumentFormatType.PLAIN_TEXT
            
            // Read file content
            val content = when (formatType) {
                DocumentFormatType.MARKDOWN, DocumentFormatType.PLAIN_TEXT -> {
                    file.readText()
                }
                else -> {
                    // For binary formats (PDF, DOCX, PPTX), read as bytes and convert to ISO-8859-1
                    // This is how Tika expects binary data
                    val bytes = file.readBytes()
                    String(bytes, Charsets.ISO_8859_1)
                }
            }
            
            // Create DocumentFile
            val metadata = DocumentMetadata(
                lastModified = file.lastModified(),
                fileSize = file.length(),
                formatType = formatType
            )
            
            val documentFile = DocumentFile(
                name = file.name,
                path = relativePath,
                metadata = metadata
            )
            
            // Parse document
            val parsedDoc = parser.parse(documentFile, content)
            
            // Register in registry
            DocumentRegistry.registerDocument(relativePath, parsedDoc, parser)
            
            true
        } catch (e: Exception) {
            System.err.println("   Failed to register $relativePath: ${e.message}")
            false
        }
    }
}

/**
 * Simple config data classes for YAML parsing
 */
@Serializable
data class AutoDevConfig(
    val active: String,
    val configs: List<LLMConfigEntry>
)

@Serializable
data class LLMConfigEntry(
    val name: String,
    val provider: String,
    val apiKey: String,
    val model: String,
    val baseUrl: String? = null,
    val temperature: Double? = null,
    val maxTokens: Int? = null
)

/**
 * Console renderer for CLI output
 */
class ConsoleRenderer : CodingAgentRenderer {
    override fun renderIterationHeader(current: Int, max: Int) {
        println("\n━━━ Iteration $current/$max ━━━")
    }
    
    override fun renderLLMResponseStart() {
        println("💭 ")
    }
    
    override fun renderLLMResponseChunk(chunk: String) {
        print(chunk)
    }
    
    override fun renderLLMResponseEnd() {
        println("\n")
    }
    
    override fun renderToolCall(toolName: String, paramsStr: String) {
        println("● $toolName")
        if (paramsStr.isNotEmpty()) {
            println("  ⎿ $paramsStr")
        }
    }
    
    override fun renderToolResult(
        toolName: String,
        success: Boolean,
        output: String?,
        fullOutput: String?,
        metadata: Map<String, String>
    ) {
        val statusSymbol = if (success) "✓" else "✗"
        val preview = (output ?: fullOutput ?: "").lines().take(3).joinToString(" ").take(100)
        println("  $statusSymbol ${if (preview.length < (output ?: fullOutput ?: "").length) "$preview..." else preview}")
    }
    
    override fun renderTaskComplete() {
        println("\n✓ Task marked as complete")
    }
    
    override fun renderFinalResult(success: Boolean, message: String, iterations: Int) {
        val symbol = if (success) "✅" else "❌"
        println("\n$symbol Final result after $iterations iterations:")
        println(message)
    }
    
    override fun renderError(message: String) {
        System.err.println("❌ Error: $message")
    }
    
    override fun renderRepeatWarning(toolName: String, count: Int) {
        println("⚠️  Warning: Tool '$toolName' called $count times")
    }
    
    override fun renderRecoveryAdvice(recoveryAdvice: String) {
        println("💡 Recovery advice: $recoveryAdvice")
    }
    
    override fun updateTokenInfo(tokenInfo: TokenInfo) {
        // Skip token info in CLI
    }
    
    override fun renderUserConfirmationRequest(toolName: String, params: Map<String, Any>) {
        println("❓ Confirmation required for: $toolName")
        println("   Params: $params")
    }
}

