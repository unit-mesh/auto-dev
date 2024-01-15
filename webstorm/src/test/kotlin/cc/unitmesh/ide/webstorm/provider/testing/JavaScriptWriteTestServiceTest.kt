package cc.unitmesh.ide.webstorm.provider.testing;

import com.intellij.lang.javascript.psi.JSExecutionScope
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory
import com.intellij.lang.typescript.compiler.TypeScriptService
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightPlatformTestCase

class JavaScriptWriteTestServiceTest: LightPlatformTestCase() {
    fun testShouldIdentifyIsExportedClass() {
        val code = """
            export class Foo {
                constructor() {
                }
            }
        """.trimIndent()

//        JSPsiElementFactory.createJSClass(code, null)
//        val scriptScope: JSExecutionScope = TypeScriptService.getForFile(project, null)
        val jsClazz = JSPsiElementFactory.createJSSourceElement(code, null as PsiElement, JSClass::class.java)
        val result = JavaScriptWriteTestService.isExportedClass(jsClazz)
    }
}