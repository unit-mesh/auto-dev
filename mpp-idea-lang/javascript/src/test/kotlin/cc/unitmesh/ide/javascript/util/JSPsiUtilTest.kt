package cc.unitmesh.ide.javascript.util;

import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightPlatformTestCase

class JSPsiUtilTest : LightPlatformTestCase() {
    fun testShouldIdentifyIsExportedClass() {
        val code = """
            export class Foo {
                constructor() {
                }
            }
        """.trimIndent()

        val file = PsiFileFactory.getInstance(project).createFileFromText(JavascriptLanguage.INSTANCE, code)
        val jsClazz = file.children.first() as JSClass
        val result = JSPsiUtil.isExportedClass(jsClazz)

        assertTrue(result)
    }

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
        val result = JSPsiUtil.isExportedClassPublicMethod(jsFunc)

        assertTrue(result)
    }
}
