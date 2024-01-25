package cc.unitmesh.ide.javascript.util;

import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.dialects.TypeScriptJSXLanguageDialect
import com.intellij.lang.javascript.dialects.TypeScriptLanguageDialect
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightPlatformTestCase

class ReactUtilTest : LightPlatformTestCase() {
    fun testShouldHandleExportReactComponent() {
        val code = """
            import type { AppProps } from 'next/app';

            const MyApp = ({ Component, pageProps }: AppProps) => (
              <Component {...pageProps} />
            );
            
            export default MyApp;
            """.trimIndent()

        val file = PsiFileFactory.getInstance(project).createFileFromText(JavascriptLanguage.INSTANCE, code)
        val result = ReactUtil.tsxComponentToComponent(file as JSFile)

        assertEquals(1, result.size)
    }
}
