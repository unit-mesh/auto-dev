package cc.unitmesh.server.cli

import cc.unitmesh.agent.CodeReviewAgent
import cc.unitmesh.agent.ReviewType
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.linter.LintFileResult
import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintResult
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.LinterRegistry
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.compression.TokenInfo
import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * JVM CLI for testing CodeReviewAgent.generateFixes with CodingAgent
 * 
 * Usage:
 * ```bash
 * ./gradlew :mpp-ui:runReviewCli -PreviewProjectPath=/path/to/project -PreviewPatch="diff..." -PreviewAnalysis="analysis..." [-PreviewUserFeedback="feedback"]
 * ```
 * 
 * Or with git diff:
 * ```bash
 * ./gradlew :mpp-ui:runReviewCli -PreviewProjectPath=/path/to/project -PreviewCommitHash=HEAD -PreviewAnalysis="analysis..."
 * ```
 */
object ReviewCli {
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("=".repeat(80))
        println("AutoDev Code Review CLI (Fix Generation with CodingAgent)")
        println("=".repeat(80))
        
        // Parse arguments
        val projectPath = System.getProperty("reviewProjectPath") ?: args.getOrNull(0) ?: run {
            System.err.println("Usage: -PreviewProjectPath=<path> -PreviewAnalysis=<analysis> [-PreviewPatch=<patch>] [-PreviewCommitHash=<hash>] [-PreviewUserFeedback=<feedback>]")
            return
        }
        val analysisOutput = System.getProperty("reviewAnalysis") ?: args.getOrNull(1) ?: run {
            System.err.println("Usage: -PreviewProjectPath=<path> -PreviewAnalysis=<analysis> [-PreviewPatch=<patch>] [-PreviewCommitHash=<hash>] [-PreviewUserFeedback=<feedback>]")
            return
        }
        val patch = System.getProperty("reviewPatch") ?: args.getOrNull(2)
        val commitHash = System.getProperty("reviewCommitHash") ?: args.getOrNull(3)
        val userFeedback = System.getProperty("reviewUserFeedback") ?: args.getOrNull(4) ?: ""
        val language = System.getProperty("reviewLanguage") ?: args.getOrNull(5) ?: "EN"
        
        println("📂 Project Path: $projectPath")
        println("📊 Analysis Output: ${analysisOutput.take(100)}...")
        if (patch != null) {
            println("📝 Patch: ${patch.length} chars")
        }
        if (commitHash != null) {
            println("🔖 Commit: $commitHash")
        }
        if (userFeedback.isNotEmpty()) {
            println("💬 User Feedback: $userFeedback")
        }
        println("🌐 Language: $language")
        println()
        
        runBlocking {
            try {
                val projectDir = File(projectPath).absoluteFile
                if (!projectDir.exists()) {
                    System.err.println("❌ Project path does not exist: $projectPath")
                    return@runBlocking
                }
                
                val startTime = System.currentTimeMillis()
                
                // Get patch from git if not provided
                val finalPatch = patch ?: if (commitHash != null) {
                    println("📥 Fetching git diff from commit: $commitHash")
                    val gitDiff = getGitDiff(projectDir, commitHash)
                    if (gitDiff == null) {
                        System.err.println("❌ Failed to get git diff for commit: $commitHash")
                        return@runBlocking
                    }
                    println("✅ Got git diff (${gitDiff.length} chars)")
                    println()
                    gitDiff
                } else {
                    System.err.println("❌ Either -PreviewPatch or -PreviewCommitHash must be provided")
                    return@runBlocking
                }
                
                // Run linters on changed files
                println("🔍 Running linters on changed files...")
                val changedFiles = extractFilePathsFromPatch(finalPatch)
                println("   Found ${changedFiles.size} changed files:")
                changedFiles.forEach { println("   • $it") }
                println()
                
                val lintResults = if (changedFiles.isNotEmpty()) {
                    runLinters(projectDir, changedFiles)
                } else {
                    emptyList()
                }
                
                println("📋 Lint Results:")
                if (lintResults.isEmpty()) {
                    println("   No lint issues found")
                } else {
                    lintResults.forEach { result ->
                        val total = result.errorCount + result.warningCount + result.infoCount
                        val symbol = when {
                            result.errorCount > 0 -> "❌"
                            result.warningCount > 0 -> "⚠️"
                            else -> "ℹ️"
                        }
                        println("   $symbol ${result.filePath}: $total issues (${result.errorCount} errors, ${result.warningCount} warnings)")
                    }
                }
                println()
                
                // Load LLM configuration
                println("🧠 Loading LLM configuration...")
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
                
                // Create CodeReviewAgent
                val reviewAgent = CodeReviewAgent(
                    projectPath = projectPath,
                    llmService = llmService,
                    maxIterations = 50,
                    renderer = renderer,
                    mcpToolConfigService = mcpConfigService,
                    enableLLMStreaming = true
                )
                
                println("✅ CodeReviewAgent created")
                println()
                
                // Execute fix generation
                println("🔧 Generating fixes using CodingAgent...")
                println()
                
                val fixStartTime = System.currentTimeMillis()
                val result = reviewAgent.generateFixes(
                    patch = finalPatch,
                    lintResults = lintResults,
                    analysisOutput = analysisOutput,
                    userFeedback = userFeedback,
                    language = language
                ) { progress ->
                    print(progress)
                    System.out.flush()
                }
                
                val fixTime = System.currentTimeMillis() - fixStartTime
                
                println()
                println()
                println("=".repeat(80))
                println("📊 Fix Generation Result:")
                println("=".repeat(80))
                
                if (result.success) {
                    println("✅ Fix generation completed successfully")
                    println("⏱️  Fix time: ${fixTime}ms")
                    println("⏱️  Total time: ${System.currentTimeMillis() - startTime}ms")
                    println("🔧 Used tools: ${result.usedTools}")
                    if (result.issuesAnalyzed.isNotEmpty()) {
                        println("📁 Files analyzed: ${result.issuesAnalyzed.size}")
                        result.issuesAnalyzed.forEach { println("   • $it") }
                    }
                    println()
                    println("📝 Generated Content:")
                    println("-".repeat(80))
                    println(result.content)
                    println("-".repeat(80))
                } else {
                    println("❌ Fix generation failed")
                    println("⏱️  Fix time: ${fixTime}ms")
                    println("⏱️  Total time: ${System.currentTimeMillis() - startTime}ms")
                    println()
                    println("❌ Error:")
                    println("-".repeat(80))
                    println(result.content)
                    println("-".repeat(80))
                }
                
            } catch (e: Exception) {
                System.err.println("❌ Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get git diff for a commit
     */
    private fun getGitDiff(projectDir: File, commitHash: String): String? {
        return try {
            val process = ProcessBuilder("git", "show", commitHash)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0 && output.isNotBlank()) {
                output
            } else {
                null
            }
        } catch (e: Exception) {
            System.err.println("⚠️  Failed to get git diff: ${e.message}")
            null
        }
    }
    
    /**
     * Extract file paths from patch
     */
    private fun extractFilePathsFromPatch(patch: String): List<String> {
        val filePaths = mutableSetOf<String>()
        val lines = patch.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            // Look for "--- a/path" or "+++ b/path" or "diff --git a/path b/path"
            when {
                line.startsWith("diff --git") -> {
                    val parts = line.split(" ")
                    if (parts.size >= 4) {
                        val oldPath = parts[2].removePrefix("a/")
                        val newPath = parts[3].removePrefix("b/")
                        if (oldPath != "/dev/null") filePaths.add(oldPath)
                        if (newPath != "/dev/null") filePaths.add(newPath)
                    }
                }
                line.startsWith("--- a/") -> {
                    val path = line.removePrefix("--- a/").split("\t").first()
                    if (path != "/dev/null") filePaths.add(path)
                }
                line.startsWith("+++ b/") -> {
                    val path = line.removePrefix("+++ b/").split("\t").first()
                    if (path != "/dev/null") filePaths.add(path)
                }
            }
            i++
        }
        
        return filePaths.toList()
    }
    
    /**
     * Run linters on changed files
     */
    private suspend fun runLinters(projectDir: File, filePaths: List<String>): List<LintFileResult> {
        val lintResults = mutableListOf<LintFileResult>()
        val absolutePaths = filePaths.map { File(projectDir, it).absolutePath }
        
        try {
            val linterRegistry = LinterRegistry.getInstance()
            val linters = linterRegistry.findLintersForFiles(absolutePaths)
            
            if (linters.isEmpty()) {
                println("   No suitable linters found for the given files")
                return emptyList()
            }
            
            println("   Running linters: ${linters.joinToString(", ") { it.name }}")
            
            // Run each linter
            for (linter in linters) {
                try {
                    val results = linter.lintFiles(absolutePaths, projectDir.absolutePath)
                    
                    // Process each result
                    for (result in results) {
                        if (!result.hasIssues) continue
                        
                        val relativePath = File(result.filePath).relativeTo(projectDir).path
                        val allIssues = result.issues
                        
                        val errorCount = allIssues.count { it.severity == LintSeverity.ERROR }
                        val warningCount = allIssues.count { it.severity == LintSeverity.WARNING }
                        val infoCount = allIssues.count { it.severity == LintSeverity.INFO }
                        
                        // Check if we already have results for this file
                        val existingIndex = lintResults.indexOfFirst { it.filePath == relativePath }
                        if (existingIndex >= 0) {
                            // Merge with existing results
                            val existing = lintResults[existingIndex]
                            lintResults[existingIndex] = existing.copy(
                                errorCount = existing.errorCount + errorCount,
                                warningCount = existing.warningCount + warningCount,
                                infoCount = existing.infoCount + infoCount,
                                issues = existing.issues + allIssues,
                                linterName = "${existing.linterName}, ${linter.name}"
                            )
                        } else {
                            lintResults.add(
                                LintFileResult(
                                    filePath = relativePath,
                                    linterName = linter.name,
                                    errorCount = errorCount,
                                    warningCount = warningCount,
                                    infoCount = infoCount,
                                    issues = allIssues
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    System.err.println("   ⚠️  Linter ${linter.name} failed: ${e.message}")
                    // Continue with other linters
                }
            }
        } catch (e: Exception) {
            System.err.println("⚠️  Failed to run linters: ${e.message}")
            // Continue without lint results
        }
        
        return lintResults
    }
}

// Reuse ConsoleRenderer and config classes from DocumentCli

