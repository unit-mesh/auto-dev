package cc.unitmesh.devti.command

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class EditFileCommandTest {

    @Test
    fun `should parse YAML format with block scalar`() {
        val content = """
            target_file: "src/main/kotlin/MyClass.kt"
            instructions: "Add a new method"
            code_edit: |
              class MyClass {
                  // ... existing code ...
                  
                  fun calculateSum(a: Int, b: Int): Int {
                      return a + b
                  }
                  
                  // ... existing code ...
              }
        """.trimIndent()

        val editFileCommand = EditFileCommand(MockProject())
        val result = editFileCommand.parseEditRequest(content)

        assertNotNull(result)
        assertEquals("src/main/kotlin/MyClass.kt", result!!.targetFile)
        assertEquals("Add a new method", result.instructions)
        assertTrue(result.codeEdit.contains("fun calculateSum"))
        assertTrue(result.codeEdit.contains("// ... existing code ..."))
    }

    @Test
    fun `should parse YAML format with complex quotes and special characters`() {
        val content = """
            target_file: "src/main/kotlin/StringUtils.kt"
            instructions: "Add method with various quote types"
            code_edit: |
              class StringUtils {
                  fun processText(): String {
                      val message = "This contains \"double quotes\" and 'single quotes'"
                      val multiline = '''
                          This is a multiline string
                          with various "quotes" and characters
                          including ${'$'}{variables} and \n escapes
                      '''
                      return message + multiline
                  }
              }
        """.trimIndent()

        val editFileCommand = EditFileCommand(MockProject())
        val result = editFileCommand.parseEditRequest(content)

        assertNotNull(result)
        assertEquals("src/main/kotlin/StringUtils.kt", result!!.targetFile)
        assertTrue(result.codeEdit.contains("\"double quotes\""))
        assertTrue(result.codeEdit.contains("'single quotes'"))
        assertTrue(result.codeEdit.contains("multiline string"))
    }

    @Test
    fun `should parse legacy format for backward compatibility`() {
        val content = """
            target_file: "src/main/kotlin/MyClass.kt"
            instructions: "Add a new method"
            code_edit: "class MyClass {
                // ... existing code ...
                
                fun calculateSum(a: Int, b: Int): Int {
                    return a + b
                }
                
                // ... existing code ...
            }"
        """.trimIndent()

        val editFileCommand = EditFileCommand(MockProject())
        val result = editFileCommand.parseEditRequest(content)

        assertNotNull(result)
        assertEquals("src/main/kotlin/MyClass.kt", result!!.targetFile)
        assertEquals("Add a new method", result.instructions)
        assertTrue(result.codeEdit.contains("fun calculateSum"))
    }

    @Test
    fun `should handle malformed content gracefully`() {
        val content = """
            target_file: "src/main/kotlin/MyClass.kt"
            instructions: "Add a new method"
            code_edit: |
              class MyClass {
                  // Missing closing brace - malformed but should still parse
                  fun calculateSum(a: Int, b: Int): Int {
                      return a + b
                  }
        """.trimIndent()

        val editFileCommand = EditFileCommand(MockProject())
        val result = editFileCommand.parseEditRequest(content)

        assertNotNull(result)
        assertEquals("src/main/kotlin/MyClass.kt", result!!.targetFile)
        assertTrue(result.codeEdit.contains("fun calculateSum"))
    }

    @Test
    fun `should return null for completely invalid content`() {
        val content = "This is not a valid edit request format"

        val editFileCommand = EditFileCommand(MockProject())
        val result = editFileCommand.parseEditRequest(content)

        assertNull(result)
    }

    @Test
    fun `should handle empty instructions field`() {
        val content = """
            target_file: "src/main/kotlin/MyClass.kt"
            code_edit: |
              class MyClass {
                  fun newMethod() {}
              }
        """.trimIndent()

        val editFileCommand = EditFileCommand(MockProject())
        val result = editFileCommand.parseEditRequest(content)

        assertNotNull(result)
        assertEquals("src/main/kotlin/MyClass.kt", result!!.targetFile)
        assertEquals("", result.instructions)
        assertTrue(result.codeEdit.contains("fun newMethod"))
    }

    @Test
    fun `should parse YAML with folded scalar`() {
        val content = """
            target_file: "src/main/kotlin/MyClass.kt"
            instructions: >
              This is a folded scalar instruction
              that spans multiple lines
              but will be joined with spaces
            code_edit: |
              class MyClass {
                  fun method() {}
              }
        """.trimIndent()

        val editFileCommand = EditFileCommand(MockProject())
        val result = editFileCommand.parseEditRequest(content)

        assertNotNull(result)
        assertEquals("src/main/kotlin/MyClass.kt", result!!.targetFile)
        assertTrue(result.instructions.contains("folded scalar instruction"))
        assertTrue(result.codeEdit.contains("fun method"))
    }
}

// Mock project class for testing
class MockProject : com.intellij.openapi.project.Project {
    override fun getName(): String = "MockProject"
    override fun getLocationHash(): String = "mock"
    override fun getProjectFilePath(): String? = null
    override fun getWorkspaceFile(): com.intellij.openapi.vfs.VirtualFile? = null
    override fun getBaseDir(): com.intellij.openapi.vfs.VirtualFile? = null
    override fun getBasePath(): String? = null
    override fun getPresentableUrl(): String? = null
    override fun getProjectFile(): com.intellij.openapi.vfs.VirtualFile? = null
    override fun isOpen(): Boolean = true
    override fun isInitialized(): Boolean = true
    override fun isDefault(): Boolean = false
    override fun save() {}
    override fun dispose() {}
    override fun <T : Any?> getUserData(key: com.intellij.openapi.util.Key<T>): T? = null
    override fun <T : Any?> putUserData(key: com.intellij.openapi.util.Key<T>, value: T?) {}
    override fun getMessageBus(): com.intellij.util.messages.MessageBus = TODO()
    override fun isDisposed(): Boolean = false
    override fun getDisposed(): com.intellij.openapi.Disposable = TODO()
    override fun <T : Any?> getService(serviceClass: Class<T>): T = TODO()
    override fun <T : Any?> getComponent(interfaceClass: Class<T>): T? = TODO()
    override fun hasComponent(interfaceClass: Class<*>): Boolean = false
    override fun <T : Any?> getServices(serviceClass: Class<T>, includeLocal: Boolean): MutableList<T> = TODO()
    override fun getPicoContainer(): org.picocontainer.PicoContainer = TODO()
    override fun getExtensionArea(): com.intellij.openapi.extensions.ExtensionsArea = TODO()
}
