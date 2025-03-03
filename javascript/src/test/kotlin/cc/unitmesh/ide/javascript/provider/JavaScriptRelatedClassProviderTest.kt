package cc.unitmesh.ide.javascript.provider

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightPlatformTestCase
import org.intellij.lang.annotations.Language

class JavaScriptRelatedClassProviderTest : LightPlatformTestCase() {
    @Language("JavaScript")
    val code = """
    function bar() {
        console.log('bar');
    }

    function baz() {
        console.log('baz');
    }
    
    function foo() {
        bar();
        baz();
    }
    """.trimIndent()

    fun testShouldReturnCorrectCallees() {
        // given
        val file = PsiFileFactory.getInstance(project).createFileFromText("test.js", code)
        val lastFunction = file.children.last()

        // when
        val provider = JavaScriptRelatedClassProvider()
        val result = provider.findCallees(project, lastFunction as JSFunction)

        // then
        assertEquals(2, result.size)
    }
}
