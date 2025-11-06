package cc.unitmesh.devins.test

import cc.unitmesh.agent.logging.getLogger

/**
 * 测试运行器
 * 用于运行 DevIns 测试用例
 */
class TestRunner {

    private val logger = getLogger("TestRunner")
    private val testCases = mutableListOf<DevInsTestCase>()
    
    /**
     * 添加测试用例
     */
    fun addTestCase(testCase: DevInsTestCase) {
        testCases.add(testCase)
    }
    
    /**
     * 运行所有测试用例
     */
    fun runAll(): TestSuiteResult {
        val results = mutableListOf<TestResult>()
        
        for (testCase in testCases) {
            try {
                val result = testCase.runTest()
                results.add(result)

                if (!result.passed) {
                    // 打印失败的详细信息
                    result.results.filter { !it.passed }.forEach { singleResult ->
                        logger.error { "  - ${singleResult.name}: ${singleResult.message}" }
                    }
                }
            } catch (e: Exception) {
                val errorResult = TestResult(
                    testName = testCase.testName,
                    passed = false,
                    totalTests = 1,
                    passedTests = 0,
                    failedTests = 1,
                    results = listOf(SingleTestResult(testCase.testName, false, "Exception: ${e.message}"))
                )
                results.add(errorResult)
            }
        }
        
        val totalTests = results.sumOf { it.totalTests }
        val passedTests = results.sumOf { it.passedTests }
        val failedTests = results.sumOf { it.failedTests }
        val allPassed = results.all { it.passed }
        
        return TestSuiteResult(
            totalSuites = testCases.size,
            passedSuites = results.count { it.passed },
            failedSuites = results.count { !it.passed },
            totalTests = totalTests,
            passedTests = passedTests,
            failedTests = failedTests,
            allPassed = allPassed,
            results = results
        )
    }
    
    /**
     * 运行指定名称的测试用例
     */
    fun runTest(testName: String): TestResult? {
        val testCase = testCases.find { it.testName == testName }
        return testCase?.runTest()
    }
    
    /**
     * 获取所有测试用例名称
     */
    fun getTestNames(): List<String> {
        return testCases.map { it.testName }
    }
    
    /**
     * 清除所有测试用例
     */
    fun clear() {
        testCases.clear()
    }
}

/**
 * 测试套件结果
 */
data class TestSuiteResult(
    val totalSuites: Int,
    val passedSuites: Int,
    val failedSuites: Int,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val allPassed: Boolean,
    val results: List<TestResult>
) {
    override fun toString(): String {
        val status = if (allPassed) "ALL PASSED" else "SOME FAILED"
        return """
            Test Suite Results: $status
            Suites: $passedSuites/$totalSuites passed
            Tests: $passedTests/$totalTests passed
        """.trimIndent()
    }
}

/**
 * 简单的测试用例实现示例
 */
class SimpleParsingTest : DevInsParsingTestCase() {
    
    override val testName: String = "Simple Parsing Test"
    
    override val testData: List<TestData> = listOf(
        createTestData(
            name = "Basic Front Matter",
            input = """
                ---
                name: "test"
                value: 42
                ---
            """.trimIndent()
        ),
        
        createTestData(
            name = "Simple Variable",
            input = "${'$'}variable"
        ),
        
        createTestData(
            name = "Simple Command",
            input = "/command:argument"
        ),
        
        createTestData(
            name = "Simple Agent",
            input = "@agent"
        ),
        
        createTestData(
            name = "Code Block",
            input = """
                ```kotlin
                fun main() {
                    println("Hello World")
                }
                ```
            """.trimIndent()
        ),
        
        createTestData(
            name = "Text Segment",
            input = "This is a simple text segment."
        )
    )
}

/**
 * 简单的词法分析测试用例实现示例
 */
class SimpleLexingTest : DevInsLexingTestCase() {
    
    override val testName: String = "Simple Lexing Test"
    
    override val testData: List<TestData> = listOf(
        createTestData(
            name = "Keywords",
            input = "when case default if else",
            expectedOutput = """
                WHEN('when')
                IDENTIFIER('case')
                DEFAULT('default')
                IF('if')
                ELSE('else')
                EOF('')
            """.trimIndent()
        ),
        
        createTestData(
            name = "Operators",
            input = ": == != < > <= >= && ||",
            expectedOutput = """
                COLON(':')
                EQEQ('==')
                NEQ('!=')
                LT('<')
                GT('>')
                LTE('<=')
                GTE('>=')
                ANDAND('&&')
                OROR('||')
                EOF('')
            """.trimIndent()
        ),
        
        createTestData(
            name = "Special Characters",
            input = "@ / ${'$'}",
            expectedOutput = """
                AGENT_START('@')
                COMMAND_START('/')
                VARIABLE_START('$')
                EOF('')
            """.trimIndent()
        )
    )
}
