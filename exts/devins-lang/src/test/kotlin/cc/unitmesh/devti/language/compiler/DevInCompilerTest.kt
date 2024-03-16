package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class DevInCompilerTest : LightJavaCodeInsightFixtureTestCase() {
    fun testNormalString() {
        val code = "Normal String /"
        val file = myFixture.configureByText("test.devin", code)

        val compile = DevInsCompiler(project, file as DevInFile, myFixture.editor).compile()
        assertEquals("Normal String /", compile.output)
    }
}

