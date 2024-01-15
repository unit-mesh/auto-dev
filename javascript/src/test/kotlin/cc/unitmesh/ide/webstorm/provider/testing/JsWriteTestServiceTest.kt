package cc.unitmesh.ide.webstorm.provider.testing;

import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightPlatformTestCase

class JsWriteTestServiceTest: LightPlatformTestCase() {
    fun testShouldIdentifyIsExportedClass() {
        val code = """
            export class Foo {
                constructor() {
                }
            }
        """.trimIndent()

        val file = PsiFileFactory.getInstance(project).createFileFromText(JavascriptLanguage.INSTANCE, code)
        val jsClazz = file.children.first() as JSClass
        val result = JavaScriptWriteTestService.isExportedClass(jsClazz)

        assertTrue(result)
    }
}