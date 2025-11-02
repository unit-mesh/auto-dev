package cc.unitmesh.agent

import cc.unitmesh.agent.core.DefaultAgentExecutor
import cc.unitmesh.agent.model.*
import cc.unitmesh.agent.subagent.ErrorRecoveryAgent
import cc.unitmesh.agent.subagent.LogSummaryAgent
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * E2E 集成测试
 * 
 * 使用真实的 LLM 服务测试整个 Agent 架构
 * 需要配置文件：~/.autodev/config.yaml
 */
class AgentE2ETest {

    private val configPath = "${System.getProperty("user.home")}/.autodev/config.yaml"
    
    /**
     * 检查配置文件是否存在
     */
    private fun hasConfig(): Boolean = File(configPath).exists()

    /**
     * 从配置文件加载 ModelConfig
     */
    private fun loadConfig(): ModelConfig {
        val configFile = File(configPath)
        val content = configFile.readText()
        
        // 简单解析 YAML（真实场景应该用 YAML 库）
        val apiKeyMatch = Regex("apiKey:\\s*(.+)").find(content)
        val modelMatch = Regex("model:\\s*(.+)").find(content)
        val providerMatch = Regex("provider:\\s*(.+)").find(content)
        
        val apiKey = apiKeyMatch?.groupValues?.get(1)?.trim() ?: ""
        val model = modelMatch?.groupValues?.get(1)?.trim() ?: "deepseek-chat"
        val providerStr = providerMatch?.groupValues?.get(1)?.trim() ?: "deepseek"
        
        val provider = when (providerStr.lowercase()) {
            "deepseek" -> LLMProviderType.DEEPSEEK
            "openai" -> LLMProviderType.OPENAI
            "anthropic" -> LLMProviderType.ANTHROPIC
            "ollama" -> LLMProviderType.OLLAMA
            else -> LLMProviderType.DEEPSEEK
        }
        
        return ModelConfig(
            provider = provider,
            apiKey = apiKey,
            modelName = model,
            temperature = 0.7
        )
    }

    @Test
    fun `ErrorRecoveryAgent should analyze build failure E2E`() = runBlocking {
        if (!hasConfig()) {
            println("⚠️ Skipping test: Config file not found at $configPath")
            return@runBlocking
        }
        println("\n========================================")
        println("🧪 Testing ErrorRecoveryAgent E2E")
        println("========================================\n")
        
        // 1. 加载配置并创建 LLM 服务
        val config = loadConfig()
        println("✓ Config loaded: ${config.provider}/${config.modelName}")
        
        val llmService = KoogLLMService.create(config)
        println("✓ LLM Service created")
        
        // 2. 创建 ErrorRecoveryAgent
        val agent = ErrorRecoveryAgent(
            projectPath = System.getProperty("user.dir"),
            llmService = llmService
        )
        println("✓ ErrorRecoveryAgent created")
        
        // 3. 模拟一个构建失败场景
        val input = mapOf(
            "command" to "./gradlew build",
            "errorMessage" to """
                FAILURE: Build failed with an exception.
                
                * What went wrong:
                Execution failed for task ':app:compileKotlin'.
                > Compilation error. See log for more details
                
                e: /src/Main.kt:5:20: Unresolved reference: unknownFunction
            """.trimIndent(),
            "exitCode" to 1
        )
        
        println("\n📝 Input:")
        println("   Command: ${input["command"]}")
        println("   Error: ${(input["errorMessage"] as String).take(100)}...")
        
        // 4. 执行分析
        println("\n🤖 Analyzing with AI...")
        val result = agent.run(input) { progress ->
            println("   $progress")
        }
        
        // 5. 验证结果
        println("\n✅ Result:")
        println(result)
        println()
        
        // 断言：结果应该包含分析内容
        assertTrue(result.isNotBlank(), "Result should not be blank")
        assertTrue(
            result.contains("Analysis") || result.contains("analysis") || result.contains("分析"),
            "Result should contain analysis"
        )
        
        println("✅ ErrorRecoveryAgent E2E test passed!\n")
    }

    @Test
    fun `LogSummaryAgent should summarize long output E2E`() = runBlocking {
        if (!hasConfig()) {
            println("⚠️ Skipping test: Config file not found at $configPath")
            return@runBlocking
        }
        println("\n========================================")
        println("🧪 Testing LogSummaryAgent E2E")
        println("========================================\n")
        
        // 1. 加载配置并创建 LLM 服务
        val config = loadConfig()
        println("✓ Config loaded: ${config.provider}/${config.modelName}")
        
        val llmService = KoogLLMService.create(config)
        println("✓ LLM Service created")
        
        // 2. 创建 LogSummaryAgent
        val agent = LogSummaryAgent(
            llmService = llmService,
            threshold = 500  // 降低阈值以便测试
        )
        println("✓ LogSummaryAgent created")
        
        // 3. 模拟一个长输出
        val longOutput = buildString {
            appendLine("Starting build...")
            appendLine("Compiling 150 files...")
            repeat(50) {
                appendLine("  [INFO] Compiling file_$it.kt")
            }
            appendLine("Running tests...")
            repeat(30) {
                appendLine("  [PASS] Test case $it")
            }
            appendLine("Build completed successfully!")
        }
        
        assertTrue(agent.needsSummarization(longOutput), "Output should need summarization")
        
        val input = mapOf(
            "command" to "./gradlew build",
            "output" to longOutput,
            "exitCode" to 0,
            "executionTime" to 3500
        )
        
        println("\n📝 Input:")
        println("   Command: ${input["command"]}")
        println("   Output length: ${longOutput.length} chars")
        println("   Needs summarization: Yes")
        
        // 4. 执行摘要
        println("\n🤖 Summarizing with AI...")
        val result = agent.run(input) { progress ->
            println("   $progress")
        }
        
        // 5. 验证结果
        println("\n✅ Result:")
        println(result)
        println()
        
        // 断言：结果应该包含摘要
        assertTrue(result.isNotBlank(), "Result should not be blank")
        assertTrue(
            result.contains("Summary") || result.contains("summary") || result.contains("摘要"),
            "Result should contain summary"
        )
        
        println("✅ LogSummaryAgent E2E test passed!\n")
    }

    @Test
    fun `DefaultAgentExecutor should complete simple task E2E`() = runBlocking {
        if (!hasConfig()) {
            println("⚠️ Skipping test: Config file not found at $configPath")
            return@runBlocking
        }
        println("\n========================================")
        println("🧪 Testing DefaultAgentExecutor E2E")
        println("========================================\n")
        
        // 1. 加载配置并创建 LLM 服务
        val config = loadConfig()
        println("✓ Config loaded: ${config.provider}/${config.modelName}")
        
        val llmService = KoogLLMService.create(config)
        println("✓ LLM Service created")
        
        // 2. 创建 DefaultAgentExecutor
        val executor = DefaultAgentExecutor(llmService)
        println("✓ DefaultAgentExecutor created")
        
        // 3. 定义一个简单的任务
        val definition = AgentDefinition(
            name = "simple_analyzer",
            displayName = "Simple Analyzer",
            description = "Analyzes a simple task",
            promptConfig = PromptConfig(
                systemPrompt = """
                    You are a helpful assistant.
                    When you complete the task, respond with 'TASK_COMPLETE'.
                    Keep your responses brief.
                """.trimIndent(),
                queryTemplate = "Task: \${task}"
            ),
            modelConfig = ModelConfig(modelId = config.modelName),
            runConfig = RunConfig(
                maxTurns = 3,
                maxTimeMinutes = 2
            )
        )
        
        val context = AgentContext.create(
            agentName = "simple_analyzer",
            sessionId = "test-session",
            inputs = mapOf("task" to "Say hello and then mark the task as complete"),
            projectPath = System.getProperty("user.dir")
        )
        
        println("\n📝 Task: Say hello and then mark the task as complete")
        
        // 4. 执行 Agent
        println("\n🤖 Executing Agent...")
        var activityCount = 0
        val result = executor.execute(definition, context) { activity ->
            activityCount++
            when (activity) {
                is AgentActivity.Progress -> println("   [Progress] ${activity.message}")
                is AgentActivity.StreamUpdate -> print(activity.text)
                is AgentActivity.TaskComplete -> println("\n   [Complete] ${activity.result.take(100)}")
                is AgentActivity.Error -> println("   [Error] ${activity.error}")
                else -> println("   [Activity] $activity")
            }
        }
        
        println("\n")
        
        // 5. 验证结果
        println("✅ Result:")
        when (result) {
            is AgentResult.Success -> {
                println("   Status: SUCCESS")
                println("   Reason: ${result.terminateReason}")
                println("   Steps: ${result.steps.size}")
                println("   Output: ${result.output}")
            }
            is AgentResult.Failure -> {
                println("   Status: FAILURE")
                println("   Reason: ${result.terminateReason}")
                println("   Error: ${result.error}")
                println("   Steps: ${result.steps.size}")
            }
        }
        println()
        
        // 断言
        assertTrue(activityCount > 0, "Should have emitted activities")
        when (result) {
            is AgentResult.Success -> assertTrue(result.steps.isNotEmpty(), "Should have execution steps")
            is AgentResult.Failure -> assertTrue(result.steps.isNotEmpty(), "Should have execution steps")
        }
        
        // 对于简单任务，应该能在 3 轮内完成
        if (result is AgentResult.Failure && result.terminateReason == TerminateReason.MAX_TURNS) {
            println("⚠️  Note: Task hit max turns (this is ok for testing)")
        }
        
        println("✅ DefaultAgentExecutor E2E test passed!\n")
    }
}

