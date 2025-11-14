package cc.unitmesh.agent.linter

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutionConfig
import cc.unitmesh.llm.KoogLLMService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AI-powered linter that generates command line scripts dynamically
 * 
 * Instead of hardcoding each linter's command format and output parsing,
 * this linter uses AI to:
 * 1. Generate the appropriate command line for any linter
 * 2. Parse the linter's output into structured issues
 * 
 * This approach is more flexible and can support any linter without code changes.
 */
class AILinter(
    private val linterName: String,
    private val linterConfig: LinterConfig,
    private val shellExecutor: ShellExecutor,
    private val llmService: KoogLLMService
) : Linter {
    
    private val logger = getLogger("AILinter")
    
    override val name: String = linterName
    override val description: String = linterConfig.description
    override val supportedExtensions: List<String> = linterConfig.fileExtensions
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override suspend fun isAvailable(): Boolean {
        return try {
            val config = ShellExecutionConfig(timeoutMs = 5000L)
            val result = shellExecutor.execute(linterConfig.versionCommand, config)
            result.exitCode == 0
        } catch (e: Exception) {
            logger.warn { "Linter $name not available: ${e.message}" }
            false
        }
    }
    
    override suspend fun lintFile(filePath: String, projectPath: String): LintResult {
        try {
            if (!isAvailable()) {
                return LintResult(
                    filePath = filePath,
                    issues = emptyList(),
                    success = false,
                    errorMessage = "Linter $name is not installed. ${getInstallationInstructions()}",
                    linterName = name
                )
            }
            
            // Step 1: Ask AI to generate the linter command
            val command = generateLinterCommand(filePath, projectPath)
            logger.info { "Generated command: $command" }
            
            // Step 2: Execute the command
            val shellConfig = ShellExecutionConfig(
                workingDirectory = projectPath,
                timeoutMs = 30000L
            )
            val result = shellExecutor.execute(command, shellConfig)
            
            // Step 3: Ask AI to parse the output
            val issues = parseOutputWithAI(result.stdout, result.stderr, filePath)
            
            return LintResult(
                filePath = filePath,
                issues = issues,
                success = true,
                errorMessage = if (result.exitCode != 0 && issues.isEmpty()) result.stderr else null,
                linterName = name
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to lint file $filePath with $name" }
            return LintResult(
                filePath = filePath,
                issues = emptyList(),
                success = false,
                errorMessage = "Failed to run linter: ${e.message}",
                linterName = name
            )
        }
    }
    
    /**
     * Use AI to generate the appropriate linter command
     */
    private suspend fun generateLinterCommand(filePath: String, projectPath: String): String {
        val prompt = buildString {
            appendLine("Generate a command line to run the $name linter on a file.")
            appendLine()
            appendLine("Linter: $name")
            appendLine("Technology: ${linterConfig.technology}")
            appendLine("File to lint: $filePath")
            appendLine("Project path: $projectPath")
            appendLine()
            appendLine("Requirements:")
            appendLine("1. The command should output results in JSON format if possible (use --format=json, --reporter=json, or similar)")
            appendLine("2. If JSON is not available, use a structured text format")
            appendLine("3. The command should be executable from the project directory")
            appendLine("4. Include the full file path in the command")
            appendLine()
            appendLine("Output ONLY the command, nothing else. Do not include explanations or markdown formatting.")
            appendLine("Example: ruff check \"$filePath\" --output-format=json")
        }
        
        val response = llmService.sendPrompt(prompt)
        return response.trim().removePrefix("```").removeSuffix("```").trim()
    }
    
    /**
     * Use AI to parse linter output into structured issues
     */
    private suspend fun parseOutputWithAI(stdout: String, stderr: String, filePath: String): List<LintIssue> {
        if (stdout.isEmpty() && stderr.isEmpty()) {
            return emptyList()
        }
        
        val prompt = buildString {
            appendLine("Parse the output from the $name linter and extract all issues.")
            appendLine()
            appendLine("Linter: $name")
            appendLine("File: $filePath")
            appendLine()
            appendLine("Linter Output (stdout):")
            appendLine("```")
            appendLine(stdout)
            appendLine("```")
            appendLine()
            if (stderr.isNotEmpty()) {
                appendLine("Linter Errors (stderr):")
                appendLine("```")
                appendLine(stderr)
                appendLine("```")
                appendLine()
            }
            appendLine("Extract all linting issues and return them as a JSON array with this structure:")
            appendLine("""
                [
                  {
                    "line": 10,
                    "column": 5,
                    "severity": "error",
                    "message": "Unused variable 'x'",
                    "rule": "no-unused-vars"
                  }
                ]
            """.trimIndent())
            appendLine()
            appendLine("Rules:")
            appendLine("1. severity must be one of: error, warning, info")
            appendLine("2. line and column should be integers (use 0 if not available)")
            appendLine("3. message should be the issue description")
            appendLine("4. rule should be the rule ID/code if available")
            appendLine("5. Return an empty array [] if no issues found")
            appendLine()
            appendLine("Output ONLY the JSON array, nothing else.")
        }
        
        try {
            val response = llmService.sendPrompt(prompt)
            val jsonText = extractJsonFromResponse(response)
            
            val parsedIssues = json.decodeFromString<List<AILintIssue>>(jsonText)
            
            return parsedIssues.map { aiIssue ->
                LintIssue(
                    line = aiIssue.line,
                    column = aiIssue.column,
                    severity = when (aiIssue.severity.lowercase()) {
                        "error" -> LintSeverity.ERROR
                        "warning" -> LintSeverity.WARNING
                        else -> LintSeverity.INFO
                    },
                    message = aiIssue.message,
                    rule = aiIssue.rule,
                    filePath = filePath
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse linter output with AI: ${e.message}" }
            // Fallback: return a generic issue indicating parsing failed
            return listOf(
                LintIssue(
                    line = 0,
                    column = 0,
                    severity = LintSeverity.INFO,
                    message = "Linter ran but output could not be parsed. Check logs for details.",
                    rule = null,
                    filePath = filePath
                )
            )
        }
    }
    
    /**
     * Extract JSON from AI response (handles markdown code blocks)
     */
    private fun extractJsonFromResponse(response: String): String {
        var text = response.trim()
        
        // Remove markdown code blocks
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json").removeSuffix("```").trim()
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```").removeSuffix("```").trim()
        }
        
        // Find JSON array
        val startIndex = text.indexOf('[')
        val endIndex = text.lastIndexOf(']')
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return text.substring(startIndex, endIndex + 1)
        }
        
        return text
    }
    
    override fun getInstallationInstructions(): String {
        return LintDetector.getInstallationInstructions(name)
    }
}

/**
 * Data class for AI-parsed lint issues
 */
@Serializable
private data class AILintIssue(
    val line: Int,
    val column: Int = 0,
    val severity: String,
    val message: String,
    val rule: String? = null
)

/**
 * Factory for creating AI-powered linters
 */
class AILinterFactory(
    private val shellExecutor: ShellExecutor,
    private val llmService: KoogLLMService,
    private val lintDetector: LintDetector
) {
    private val logger = getLogger("AILinterFactory")
    
    /**
     * Create an AI linter for a specific linter name
     */
    fun createLinter(linterName: String): AILinter? {
        val config = lintDetector.getLinterConfig(linterName)
        if (config == null) {
            logger.warn { "No configuration found for linter: $linterName" }
            return null
        }
        
        return AILinter(
            linterName = linterName,
            linterConfig = config,
            shellExecutor = shellExecutor,
            llmService = llmService
        )
    }
    
    /**
     * Create AI linters for all detected available linters
     */
    suspend fun createAvailableLinters(): List<AILinter> {
        val detectionResults = lintDetector.detectAvailableLinters()
        
        return detectionResults
            .filter { it.isAvailable }
            .mapNotNull { result ->
                createLinter(result.linterName)
            }
    }
    
    /**
     * Create AI linters for specific files
     */
    suspend fun createLintersForFiles(filePaths: List<String>): List<AILinter> {
        val detectionResults = lintDetector.detectLintersForFiles(filePaths)
        
        return detectionResults
            .filter { it.isAvailable }
            .mapNotNull { result ->
                createLinter(result.linterName)
            }
    }
}

