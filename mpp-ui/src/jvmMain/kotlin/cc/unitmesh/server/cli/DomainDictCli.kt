package cc.unitmesh.server.cli

import cc.unitmesh.agent.subagent.DomainDictAgent
import cc.unitmesh.agent.subagent.DomainDictContext
import cc.unitmesh.codegraph.parser.jvm.JvmCodeParser
import cc.unitmesh.devins.filesystem.DefaultFileSystem
import cc.unitmesh.indexer.DomainDictService
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * JVM CLI for testing DomainDictAgent output
 * 
 * Purpose: Analyze what information DomainDictAgent produces and verify
 * which details should be displayed to users in the UI.
 * 
 * Usage:
 * ```bash
 * ./gradlew :mpp-ui:runDomainDictCli -PdomainProjectPath=/path/to/project [-PdomainQuery="custom query"] [-PdomainFocusArea="agent"]
 * ```
 */
object DomainDictCli {
    
    @JvmStatic
    fun main(args: Array<String>) {
        println()
        println("=".repeat(80))
        println("AutoDev Domain Dictionary Deep Research CLI")
        println("=".repeat(80))
        println()
        
        // Parse arguments
        val projectPath = System.getProperty("domainProjectPath") ?: args.getOrNull(0) ?: run {
            System.err.println("Usage: -PdomainProjectPath=<path> [-PdomainQuery=<query>] [-PdomainFocusArea=<area>]")
            return
        }
        val userQuery = System.getProperty("domainQuery") ?: args.getOrNull(1) 
            ?: "Analyze codebase and optimize domain dictionary"
        val focusArea = System.getProperty("domainFocusArea") ?: args.getOrNull(2)
        
        println("Configuration:")
        println("  Project Path: $projectPath")
        println("  Query: $userQuery")
        println("  Focus Area: ${focusArea ?: "(none - full scan)"}")
        println()
        
        runBlocking {
            try {
                val startTime = System.currentTimeMillis()
                
                // Load LLM configuration
                println("Loading LLM configuration...")
                val configFile = File(System.getProperty("user.home"), ".autodev/config.yaml")
                if (!configFile.exists()) {
                    System.err.println("Configuration file not found: ${configFile.absolutePath}")
                    System.err.println("Please create ~/.autodev/config.yaml with your LLM configuration")
                    return@runBlocking
                }
                
                val yamlContent = configFile.readText()
                val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
                val config = yaml.decodeFromString(AutoDevConfig.serializer(), yamlContent)
                
                val activeName = config.active
                val activeConfig = config.configs.find { it.name == activeName }
                
                if (activeConfig == null) {
                    System.err.println("Active configuration '$activeName' not found in config.yaml")
                    return@runBlocking
                }
                
                println("Using LLM: ${activeConfig.provider}/${activeConfig.model}")
                println()
                
                // Create services
                val providerType = parseProviderType(activeConfig.provider)
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
                
                val fileSystem = DefaultFileSystem(projectPath)
                val domainDictService = DomainDictService(fileSystem)
                
                // Load current dictionary
                val currentDict = domainDictService.loadContent()
                val currentEntryCount = currentDict?.lines()?.count { it.contains(",") } ?: 0
                println("Current dictionary entries: $currentEntryCount")
                println()
                
                // Create agent with TreeSitter parser for deep code analysis
                println("Creating DomainDictAgent with TreeSitter parser...")
                val codeParser = JvmCodeParser()
                val agent = DomainDictAgent(
                    llmService = llmService,
                    fileSystem = fileSystem,
                    domainDictService = domainDictService,
                    codeParser = codeParser,
                    maxDefaultIterations = 7
                )
                
                println()
                println("=".repeat(80))
                println("STARTING DEEP RESEARCH")
                println("=".repeat(80))
                println()
                
                // Track timing for each progress message
                var lastProgressTime = System.currentTimeMillis()
                var progressCount = 0
                
                // Execute with detailed progress tracking
                val result = agent.execute(
                    input = DomainDictContext(
                        userQuery = userQuery,
                        focusArea = focusArea,
                        maxIterations = 7
                    ),
                    onProgress = { message ->
                        val now = System.currentTimeMillis()
                        val delta = now - lastProgressTime
                        lastProgressTime = now
                        progressCount++
                        
                        // Format output with timing
                        val prefix = when {
                            message.startsWith("## Step") -> "\n${"-".repeat(60)}\n"
                            message.startsWith("###") -> "\n"
                            message.startsWith("=") -> ""
                            else -> "  "
                        }
                        
                        val timing = if (delta > 100) " (+${delta}ms)" else ""
                        println("$prefix[$progressCount] $message$timing")
                    }
                )
                
                val totalTime = System.currentTimeMillis() - startTime
                
                println()
                println("=".repeat(80))
                println("RESEARCH COMPLETE")
                println("=".repeat(80))
                println()
                
                // Print result summary
                println("Result: ${if (result.success) "SUCCESS" else "FAILED"}")
                println("Total Time: ${totalTime}ms (${totalTime / 1000}s)")
                println("Progress Messages: $progressCount")
                println()
                
                // Print metadata
                println("Metadata:")
                result.metadata.forEach { (key, value) ->
                    println("  $key: $value")
                }
                println()
                
                // Print final report (truncated for CLI)
                println("=".repeat(80))
                println("FINAL REPORT (first 100 lines)")
                println("=".repeat(80))
                result.content.lines().take(100).forEach { println(it) }
                if (result.content.lines().size > 100) {
                    println("... (${result.content.lines().size - 100} more lines)")
                }
                
                // Print new dictionary entry count
                val newDict = domainDictService.loadContent()
                val newEntryCount = newDict?.lines()?.count { it.contains(",") } ?: 0
                println()
                println("=".repeat(80))
                println("DICTIONARY SUMMARY")
                println("=".repeat(80))
                println("Before: $currentEntryCount entries")
                println("After: $newEntryCount entries")
                println("Added: ${newEntryCount - currentEntryCount} entries")
                
            } catch (e: Exception) {
                System.err.println("Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun parseProviderType(provider: String): LLMProviderType {
        return when (provider.lowercase()) {
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
    }
}

