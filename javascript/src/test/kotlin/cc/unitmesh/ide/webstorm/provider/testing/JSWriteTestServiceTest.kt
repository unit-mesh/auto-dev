package cc.unitmesh.ide.webstorm.provider.testing;

import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightPlatformTestCase

class JSWriteTestServiceTest: LightPlatformTestCase() {
    fun testShouldIdentifyIsExportedClass() {
        val code = """
            export class Foo {
                constructor() {
                }
            }
        """.trimIndent()

        val file = PsiFileFactory.getInstance(project).createFileFromText(JavascriptLanguage.INSTANCE, code)
        val jsClazz = file.children.first() as JSClass
        val result = JSWriteTestService.isExportedClass(jsClazz)

        assertTrue(result)
    }


    // test JavaScriptWriteTestService.isExportedClassPublicMethod
    fun testShouldIdentifyIsExportedClassPublicMethod() {
        val code = """
            export class Foo {
              bar() {
              }
            }
            """.trimIndent()

        val file = PsiFileFactory.getInstance(project).createFileFromText(JavascriptLanguage.INSTANCE, code)
        val jsClazz = PsiTreeUtil.findChildOfType(file, JSClass::class.java)!!
        val jsFunc = PsiTreeUtil.findChildOfType(jsClazz, JSFunction::class.java)!!
        val result = JSWriteTestService.isExportedClassPublicMethod(jsFunc)

        assertTrue(result)
    }
}
