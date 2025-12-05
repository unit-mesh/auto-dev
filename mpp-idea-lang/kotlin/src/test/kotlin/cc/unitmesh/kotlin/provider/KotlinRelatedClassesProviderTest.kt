package cc.unitmesh.kotlin.provider

import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightPlatformTestCase
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinRelatedClassProviderTest : LightPlatformTestCase() {
    @Language("Kotlin")
    val code = """
    fun bar() {
        println("bar")
    }

    fun baz() {
        println("baz")
    }
    
    fun foo() {
        bar()
        baz()
    }
    """.trimIndent()

    fun testShouldReturnCorrectCallees() {
        // given
        val file = PsiFileFactory.getInstance(project).createFileFromText("test.kt", code)
        val lastFunction = file.children.last()

        // when
        val result = KotlinRelatedClassProvider().findCallees(project, lastFunction as KtNamedFunction)

        // then
        assertEquals(2, result.size)
    }
}