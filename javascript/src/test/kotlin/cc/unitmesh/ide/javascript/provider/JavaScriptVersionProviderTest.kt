package cc.unitmesh.ide.javascript.provider;

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.provider.context.ChatCreationContext
import cc.unitmesh.devti.provider.context.ChatOrigin
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.runBlocking

class JavaScriptVersionProviderTest : LightPlatformTestCase() {
    private val code = """
            export class Foo {
                constructor() {
                }
            }
        """.trimIndent()

    fun `testShould return true when isApplicable is called with supported creationContext`() {
        // given
        val file: PsiFile =
            PsiFileFactory.getInstance(project).createFileFromText(JavascriptLanguage.INSTANCE, code)
        val jsClazz = file.children.first() as JSClass
        val creationContext = ChatCreationContext(
            ChatOrigin.ChatAction,
            ChatActionType.GENERATE_TEST,
            sourceFile = file,
            element = jsClazz
        )

        // when
        val provider = JavaScriptVersionProvider()
        val result = provider.isApplicable(project, creationContext)

        // then
        assertTrue(result)
    }

    fun testShouldReturnPreferTypeScriptWhenCallWithCollect() {
        // given
        val file: PsiFile =
            PsiFileFactory.getInstance(project).createFileFromText(JavascriptLanguage.INSTANCE, code)

        val jsClazz = file.children.first() as JSClass
        val creationContext = ChatCreationContext(
            ChatOrigin.ChatAction,
            ChatActionType.GENERATE_TEST,
            sourceFile = file,
            element = jsClazz
        )

        // when
        val provider = JavaScriptVersionProvider()
        val result = runBlocking { provider.collect(project, creationContext) }

        // then
        assertEquals("Prefer JavaScript language if the used language and toolset.", result.first().text)
    }
}
