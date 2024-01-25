package cc.unitmesh.ide.javascript.util;

import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightPlatformTestCase

class ReactPsiUtilTest : LightPlatformTestCase() {
    fun testShouldHandleExportReactComponent() {
        val code = """
            interface AppProps {
              Component: any;
              pageProps: any;
            }

            const MyApp = ({ Component, pageProps }: AppProps) => (
              <Component {...pageProps} />
            );
            
            export default MyApp;
            """.trimIndent()

        val file = PsiFileFactory.getInstance(project).createFileFromText(JavascriptLanguage.INSTANCE, code)
        val result = ReactPsiUtil.tsxComponentToComponent(file as JSFile)

        assertEquals(1, result.size)
        assertEquals("MyApp", result.first().name)
        assertEquals("({ Component, pageProps }: AppProps)", result.first().signature)
    }
}
