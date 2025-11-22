package cc.unitmesh.devins.ui.compose.agent.codereview

import cc.unitmesh.codegraph.CodeGraphFactory
import cc.unitmesh.codegraph.model.CodeElementType
import cc.unitmesh.codegraph.model.CodeNode
import cc.unitmesh.codegraph.parser.Language
import cc.unitmesh.devins.workspace.Workspace

/**
 * Interface for finding test files related to source files
 */
interface TestFinder {
    /**
     * Find test files related to the given source file
     * 
     * @param sourceFile Path to the source file (relative to project root)
     * @param workspace Project workspace
     * @return List of related test files
     */
    suspend fun findTestFiles(sourceFile: String, workspace: Workspace): List<TestFileInfo>
    
    /**
     * Check if this finder is applicable for the given language
     */
    fun isApplicable(language: String?): Boolean
}

/**
 * Factory to create appropriate TestFinder based on language
 */
object TestFinderFactory {
    private val finders = listOf(
        JavaKotlinTestFinder(),
        PythonTestFinder(),
        JavaScriptTestFinder()
    )
    
    /**
     * Get test finder for the given language
     */
    fun getTestFinder(language: String?): TestFinder? {
        return finders.firstOrNull { it.isApplicable(language) }
    }
    
    /**
     * Find tests using all applicable finders
     */
    suspend fun findTests(sourceFile: String, language: String?, workspace: Workspace): List<TestFileInfo> {
        val finder = getTestFinder(language) ?: return emptyList()
        return finder.findTestFiles(sourceFile, workspace)
    }
}

/**
 * Base implementation for test finders
 */
abstract class BaseTestFinder : TestFinder {
    /**
     * Parse test file and extract test structure using mpp-codegraph
     */
    protected suspend fun parseTestFile(
        filePath: String,
        language: Language,
        workspace: Workspace
    ): TestFileInfo {
        return try {
            val content = workspace.fileSystem.readFile(filePath)
            if (content == null) {
                return TestFileInfo(
                    filePath = filePath,
                    language = language.name,
                    testCases = emptyList(),
                    exists = false
                )
            }
            
            val parser = CodeGraphFactory.createParser()
            val nodes = parser.parseNodes(content, filePath, language)
            
            // Build tree structure from nodes
            val testCases = buildTestTree(nodes)
            
            TestFileInfo(
                filePath = filePath,
                language = language.name,
                testCases = testCases,
                exists = true
            )
        } catch (e: Exception) {
            TestFileInfo(
                filePath = filePath,
                language = language.name,
                testCases = emptyList(),
                exists = true,
                parseError = e.message
            )
        }
    }
    
    /**
     * Build hierarchical test tree from flat list of code nodes
     */
    private fun buildTestTree(nodes: List<CodeNode>): List<TestCaseNode> {
        // Find all test classes (top-level and nested)
        val classNodes = nodes.filter { 
            it.type == CodeElementType.CLASS || it.type == CodeElementType.INTERFACE 
        }
        
        // Find all test methods/functions
        val methodNodes = nodes.filter { 
            it.type == CodeElementType.METHOD || it.type == CodeElementType.FUNCTION 
        }
        
        // If no classes found, return methods as top-level nodes (for languages like Python)
        if (classNodes.isEmpty()) {
            return methodNodes.map { method ->
                TestCaseNode(
                    name = method.name,
                    type = TestNodeType.METHOD,
                    startLine = method.startLine,
                    endLine = method.endLine
                )
            }
        }
        
        // Build tree: classes with their methods as children
        return classNodes.map { classNode ->
            val classMethods = methodNodes.filter { method ->
                // Check if method belongs to this class based on line ranges
                method.startLine >= classNode.startLine && method.endLine <= classNode.endLine
            }
            
            val childMethods = classMethods.map { method ->
                TestCaseNode(
                    name = method.name,
                    type = TestNodeType.METHOD,
                    startLine = method.startLine,
                    endLine = method.endLine
                )
            }
            
            TestCaseNode(
                name = classNode.name,
                type = TestNodeType.CLASS,
                children = childMethods,
                startLine = classNode.startLine,
                endLine = classNode.endLine,
                qualifiedName = classNode.qualifiedName
            )
        }
    }
    
    /**
     * Convert relative path from src to test
     * E.g., "src/main/java/com/example/Foo.java" -> "src/test/java/com/example/FooTest.java"
     */
    protected fun convertPathToTest(sourcePath: String, mainDir: String, testDir: String, suffix: String): String {
        return sourcePath.replace(mainDir, testDir).replace(Regex("\\.(\\w+)$")) { match ->
            suffix + match.value
        }
    }
}

/**
 * Test finder for Java and Kotlin projects
 */
class JavaKotlinTestFinder : BaseTestFinder() {
    override fun isApplicable(language: String?): Boolean {
        return language?.lowercase() in listOf("java", "kotlin")
    }
    
    override suspend fun findTestFiles(sourceFile: String, workspace: Workspace): List<TestFileInfo> {
        val results = mutableListOf<TestFileInfo>()
        
        // Determine language
        val language = when {
            sourceFile.endsWith(".java") -> Language.JAVA
            sourceFile.endsWith(".kt") -> Language.KOTLIN
            else -> return emptyList()
        }
        
        // Try different test path conventions
        val testPaths = listOf(
            // Standard Maven/Gradle: src/main/java -> src/test/java
            convertPathToTest(sourceFile, "/src/main/java/", "/src/test/java/", "Test"),
            convertPathToTest(sourceFile, "/src/main/kotlin/", "/src/test/kotlin/", "Test"),
            // Alternative: append Test to filename
            convertPathToTest(sourceFile, "/main/", "/test/", "Test"),
            // Kotlin multiplatform: commonMain -> commonTest, jvmMain -> jvmTest
            convertPathToTest(sourceFile, "/commonMain/", "/commonTest/", "Test"),
            convertPathToTest(sourceFile, "/jvmMain/", "/jvmTest/", "Test"),
        )
        
        for (testPath in testPaths) {
            val testFile = parseTestFile(testPath, language, workspace)
            if (testFile.exists) {
                results.add(testFile)
            }
        }
        
        return results
    }
}

/**
 * Test finder for Python projects
 */
class PythonTestFinder : BaseTestFinder() {
    override fun isApplicable(language: String?): Boolean {
        return language?.lowercase() == "python"
    }
    
    override suspend fun findTestFiles(sourceFile: String, workspace: Workspace): List<TestFileInfo> {
        val results = mutableListOf<TestFileInfo>()
        
        // Python test conventions:
        // 1. test_foo.py for foo.py
        // 2. foo_test.py for foo.py
        // 3. tests/test_foo.py for src/foo.py
        
        val baseName = sourceFile.substringAfterLast("/").removeSuffix(".py")
        val dir = sourceFile.substringBeforeLast("/", "")
        
        val testPaths = listOf(
            "$dir/test_$baseName.py",
            "$dir/${baseName}_test.py",
            "tests/test_$baseName.py",
            "test/test_$baseName.py",
        )
        
        for (testPath in testPaths) {
            val testFile = parseTestFile(testPath, Language.PYTHON, workspace)
            if (testFile.exists) {
                results.add(testFile)
            }
        }
        
        return results
    }
}

/**
 * Test finder for JavaScript/TypeScript projects
 */
class JavaScriptTestFinder : BaseTestFinder() {
    override fun isApplicable(language: String?): Boolean {
        return language?.lowercase() in listOf("javascript", "typescript", "jsx", "tsx")
    }
    
    override suspend fun findTestFiles(sourceFile: String, workspace: Workspace): List<TestFileInfo> {
        val results = mutableListOf<TestFileInfo>()
        
        // Determine language and extension
        val (language, extension) = when {
            sourceFile.endsWith(".ts") -> Language.TYPESCRIPT to ".ts"
            sourceFile.endsWith(".tsx") -> Language.TYPESCRIPT to ".tsx"
            sourceFile.endsWith(".jsx") -> Language.JAVASCRIPT to ".jsx"
            else -> Language.JAVASCRIPT to ".js"
        }
        
        // JavaScript test conventions:
        // 1. foo.test.js/foo.spec.js
        // 2. __tests__/foo.js
        // 3. foo.test.ts for TypeScript
        
        val baseName = sourceFile.substringAfterLast("/").removeSuffix(extension)
        val dir = sourceFile.substringBeforeLast("/", "")
        
        val testPaths = listOf(
            "$dir/$baseName.test$extension",
            "$dir/$baseName.spec$extension",
            "$dir/__tests__/$baseName$extension",
            "$dir/__tests__/$baseName.test$extension",
        )
        
        for (testPath in testPaths) {
            val testFile = parseTestFile(testPath, language, workspace)
            if (testFile.exists) {
                results.add(testFile)
            }
        }
        
        return results
    }
}
