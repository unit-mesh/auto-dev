package cc.unitmesh.ide.javascript.provider.testing;

import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightPlatformTestCase
import java.io.File

class JSWriteTestServiceTest: LightPlatformTestCase() {
    fun testShouldReturnNullWhenFilePathEmpty() {
        // given
        val code = """
            export class Foo {
                constructor() {
                }
            }
        """.trimIndent()

        val file = PsiFileFactory.getInstance(project).createFileFromText(JavascriptLanguage.INSTANCE, code)

        // when
        val result = JSWriteTestService.Util.getTestFilePath(file)

        // then
        assertEquals("null", result.toString())
    }

    fun testShouldReturnCorrectPath() {
        // given
        val code = """
            export class Foo {
                constructor() {
                }
            }
        """.trimIndent()

        val fileType = JavaScriptFileType.INSTANCE
        val file = PsiFileFactory.getInstance(project).createFileFromText(
            "parent" + File.separator + "Foo." + fileType.defaultExtension,
            fileType,
            code,
        )

        // when
        val result = JSWriteTestService.Util.getTestFilePath(file)

        // then
        assertEquals("null", result.toString())
    }
}
