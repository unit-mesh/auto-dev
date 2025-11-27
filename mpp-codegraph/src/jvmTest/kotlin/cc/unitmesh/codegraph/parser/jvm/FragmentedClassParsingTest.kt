package cc.unitmesh.codegraph.parser.jvm

import cc.unitmesh.codegraph.model.CodeElementType
import cc.unitmesh.codegraph.parser.Language
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for handling fragmented class declarations in Kotlin.
 * 
 * When Tree-sitter fails to parse large Kotlin files correctly, it may output
 * class declarations as separate nodes (class keyword + identifier + constructor)
 * instead of a proper class_declaration node. These tests verify that our parser
 * correctly reconstructs such fragmented class declarations.
 */
class FragmentedClassParsingTest {
    
    private val parser = JvmCodeParser()
    
    @Test
    fun `should parse simple data class`() = runBlocking {
        val sourceCode = """
            package cc.unitmesh.test
            
            data class FileInfo(
                val path: String,
                val name: String = ""
            )
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "test.kt", Language.KOTLIN)
        
        val classes = nodes.filter { it.type == CodeElementType.CLASS }
        assertEquals(1, classes.size, "Should find exactly 1 class")
        assertEquals("FileInfo", classes[0].name)
    }
    
    @Test
    fun `should parse normal class with constructor`() = runBlocking {
        val sourceCode = """
            package cc.unitmesh.test
            
            class DocQLExecutor(
                private val documentFile: String?,
                private val parserService: String?
            ) {
                fun execute(): String {
                    return "hello"
                }
            }
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "test.kt", Language.KOTLIN)
        
        val classes = nodes.filter { it.type == CodeElementType.CLASS }
        assertEquals(1, classes.size, "Should find exactly 1 class")
        assertEquals("DocQLExecutor", classes[0].name)
        
        val methods = nodes.filter { it.type == CodeElementType.METHOD }
        assertEquals(1, methods.size, "Should find exactly 1 method")
        assertEquals("execute", methods[0].name)
    }
    
    @Test
    fun `should parse multiple classes in same file`() = runBlocking {
        val sourceCode = """
            package cc.unitmesh.test
            
            data class FileInfo(val path: String)
            
            data class CodeBlock(val language: String?)
            
            class DocQLExecutor(private val file: String?) {
                fun execute(): String = "hello"
            }
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "test.kt", Language.KOTLIN)
        
        val classes = nodes.filter { it.type == CodeElementType.CLASS }
        assertEquals(3, classes.size, "Should find 3 classes")
        
        val classNames = classes.map { it.name }.toSet()
        assertTrue(classNames.contains("FileInfo"), "Should find FileInfo class")
        assertTrue(classNames.contains("CodeBlock"), "Should find CodeBlock class")
        assertTrue(classNames.contains("DocQLExecutor"), "Should find DocQLExecutor class")
    }
    
    @Test
    fun `should parse actual DocQLExecutor file from project`() = runBlocking {
        // Read actual file from project to test real-world scenario
        val file = java.io.File("/Volumes/source/ai/autocrud/mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/document/docql/DocQLExecutor.kt")
        if (!file.exists()) {
            println("Skipping test: DocQLExecutor.kt not found at expected path")
            return@runBlocking
        }
        
        val sourceCode = file.readText()
        val nodes = parser.parseNodes(sourceCode, "DocQLExecutor.kt", Language.KOTLIN)
        
        val classes = nodes.filter { it.type == CodeElementType.CLASS }
        
        // Should find at least these classes
        val expectedClasses = setOf("DocQLExecutor", "FileInfo", "CodeBlock", "TableBlock")
        val actualClassNames = classes.map { it.name }.toSet()
        
        println("Found classes: $actualClassNames")
        
        for (expected in expectedClasses) {
            assertTrue(
                actualClassNames.contains(expected),
                "Should find $expected class, but found: $actualClassNames"
            )
        }
    }
    
    @Test
    fun `should not create duplicate class nodes`() = runBlocking {
        val sourceCode = """
            package cc.unitmesh.test
            
            class DocQLExecutor(private val file: String?) {
                fun execute(): String = "hello"
            }
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "test.kt", Language.KOTLIN)
        
        val classes = nodes.filter { it.type == CodeElementType.CLASS }
        assertEquals(1, classes.size, "Should find exactly 1 class, not duplicates")
        assertEquals("DocQLExecutor", classes[0].name)
        
        // Ensure no 'unknown' class nodes
        val unknownClasses = classes.filter { it.name == "unknown" }
        assertEquals(0, unknownClasses.size, "Should not have any 'unknown' class nodes")
    }
    
    @Test
    fun `should correctly identify class line numbers`() = runBlocking {
        val sourceCode = """
            package cc.unitmesh.test
            
            // Line 3: comment
            // Line 4: another comment
            class MyClass(val name: String) {  // Line 5
                fun method() {}  // Line 6
            }  // Line 7
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "test.kt", Language.KOTLIN)
        
        val myClass = nodes.find { it.type == CodeElementType.CLASS && it.name == "MyClass" }
        assertTrue(myClass != null, "Should find MyClass")
        assertEquals(5, myClass!!.startLine, "MyClass should start at line 5")
    }
}

