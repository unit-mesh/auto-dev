package cc.unitmesh.devins.test

import cc.unitmesh.devins.ast.DevInsFileNode
import cc.unitmesh.devins.ast.DevInsNode
import cc.unitmesh.devins.lexer.DevInsLexer
import cc.unitmesh.devins.parser.DevInsParser
import cc.unitmesh.devins.parser.ParseResult
import cc.unitmesh.devins.token.DevInsToken

/**
 * DevIns 测试用例基类
 * 提供类似于 IDEA 平台测试的功能
 */
abstract class DevInsTestCase {
    
    /**
     * 测试用例名称
     */
    abstract val testName: String
    
    /**
     * 执行测试
     */
    abstract fun runTest(): TestResult
    
    /**
     * 断言条件为真
     */
    protected fun assertTrue(condition: Boolean, message: String = "Assertion failed") {
        if (!condition) {
            throw AssertionError(message)
        }
    }
    
    /**
     * 断言条件为假
     */
    protected fun assertFalse(condition: Boolean, message: String = "Assertion failed") {
        assertTrue(!condition, message)
    }
    
    /**
     * 断言两个值相等
     */
    protected fun <T> assertEquals(expected: T, actual: T, message: String = "Values are not equal") {
        if (expected != actual) {
            throw AssertionError("$message. Expected: $expected, Actual: $actual")
        }
    }
    
    /**
     * 断言值不为空
     */
    protected fun <T> assertNotNull(value: T?, message: String = "Value is null"): T {
        if (value == null) {
            throw AssertionError(message)
        }
        return value
    }
    
    /**
     * 断言值为空
     */
    protected fun assertNull(value: Any?, message: String = "Value is not null") {
        if (value != null) {
            throw AssertionError("$message. Actual: $value")
        }
    }
    
    /**
     * 断言解析成功
     */
    protected fun assertParseSuccess(result: ParseResult<DevInsFileNode>, message: String = "Parse failed") {
        assertTrue(result.isSuccess, "$message: ${(result as? ParseResult.Failure)?.error}")
    }
    
    /**
     * 断言解析失败
     */
    protected fun assertParseFailure(result: ParseResult<DevInsFileNode>, message: String = "Parse should have failed") {
        assertTrue(result.isFailure, message)
    }
    
    /**
     * 解析源代码
     */
    protected fun parseSource(source: String): ParseResult<DevInsFileNode> {
        val parser = DevInsParser(source)
        return parser.parse()
    }
    
    /**
     * 词法分析源代码
     */
    protected fun tokenizeSource(source: String): List<DevInsToken> {
        val lexer = DevInsLexer(source)
        return lexer.tokenize()
    }
    
    /**
     * 创建测试数据
     */
    protected fun createTestData(name: String, input: String, expectedOutput: String? = null): TestData {
        return TestData(name, input, expectedOutput)
    }
}

/**
 * 解析测试用例
 * 专门用于测试解析功能
 */
abstract class DevInsParsingTestCase : DevInsTestCase() {
    
    /**
     * 测试数据列表
     */
    abstract val testData: List<TestData>
    
    override fun runTest(): TestResult {
        val results = mutableListOf<SingleTestResult>()
        
        for (data in testData) {
            try {
                val result = testSingleCase(data)
                results.add(result)
            } catch (e: Exception) {
                results.add(SingleTestResult(data.name, false, "Exception: ${e.message}"))
            }
        }
        
        val passed = results.count { it.passed }
        val total = results.size
        
        return TestResult(
            testName = testName,
            passed = passed == total,
            totalTests = total,
            passedTests = passed,
            failedTests = total - passed,
            results = results
        )
    }
    
    /**
     * 测试单个用例
     */
    protected open fun testSingleCase(data: TestData): SingleTestResult {
        val parseResult = parseSource(data.input)
        
        return if (parseResult.isSuccess) {
            val ast = parseResult.getOrThrow()
            val actualOutput = formatAst(ast)
            
            if (data.expectedOutput != null) {
                if (actualOutput.trim() == data.expectedOutput.trim()) {
                    SingleTestResult(data.name, true, "Parse successful")
                } else {
                    SingleTestResult(data.name, false, "Output mismatch.\nExpected:\n${data.expectedOutput}\nActual:\n$actualOutput")
                }
            } else {
                SingleTestResult(data.name, true, "Parse successful")
            }
        } else {
            val error = (parseResult as ParseResult.Failure).error
            SingleTestResult(data.name, false, "Parse failed: $error")
        }
    }
    
    /**
     * 格式化 AST 为字符串
     */
    protected open fun formatAst(ast: DevInsFileNode): String {
        return AstFormatter().format(ast)
    }
}

/**
 * 词法分析测试用例
 */
abstract class DevInsLexingTestCase : DevInsTestCase() {
    
    /**
     * 测试数据列表
     */
    abstract val testData: List<TestData>
    
    override fun runTest(): TestResult {
        val results = mutableListOf<SingleTestResult>()
        
        for (data in testData) {
            try {
                val result = testSingleCase(data)
                results.add(result)
            } catch (e: Exception) {
                results.add(SingleTestResult(data.name, false, "Exception: ${e.message}"))
            }
        }
        
        val passed = results.count { it.passed }
        val total = results.size
        
        return TestResult(
            testName = testName,
            passed = passed == total,
            totalTests = total,
            passedTests = passed,
            failedTests = total - passed,
            results = results
        )
    }
    
    /**
     * 测试单个用例
     */
    protected open fun testSingleCase(data: TestData): SingleTestResult {
        val tokens = tokenizeSource(data.input)
        val actualOutput = formatTokens(tokens)
        
        return if (data.expectedOutput != null) {
            if (actualOutput.trim() == data.expectedOutput.trim()) {
                SingleTestResult(data.name, true, "Tokenization successful")
            } else {
                SingleTestResult(data.name, false, "Output mismatch.\nExpected:\n${data.expectedOutput}\nActual:\n$actualOutput")
            }
        } else {
            SingleTestResult(data.name, true, "Tokenization successful")
        }
    }
    
    /**
     * 格式化 Token 列表为字符串
     */
    protected open fun formatTokens(tokens: List<DevInsToken>): String {
        return tokens.joinToString("\n") { token ->
            "${token.type}('${token.text}')"
        }
    }
}

/**
 * 测试数据
 */
data class TestData(
    val name: String,
    val input: String,
    val expectedOutput: String? = null
)

/**
 * 测试结果
 */
data class TestResult(
    val testName: String,
    val passed: Boolean,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val results: List<SingleTestResult>
) {
    override fun toString(): String {
        val status = if (passed) "PASSED" else "FAILED"
        return "$testName: $status ($passedTests/$totalTests passed)"
    }
}

/**
 * 单个测试结果
 */
data class SingleTestResult(
    val name: String,
    val passed: Boolean,
    val message: String
) {
    override fun toString(): String {
        val status = if (passed) "PASS" else "FAIL"
        return "$name: $status - $message"
    }
}
