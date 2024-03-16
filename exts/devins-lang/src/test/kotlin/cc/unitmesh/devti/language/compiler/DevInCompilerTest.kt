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

    fun testForWriting() {
        // add fake code to project
        myFixture.configureByText("Sample.text", "Sample Text")
        val code = "/write:Sample.text#L1-L2\n```devin\nNormal String /\n```"
        val file = myFixture.configureByText("test.devin", code)

        val compile = DevInsCompiler(project, file as DevInFile, myFixture.editor).compile()
        println(compile.output)
    }
}

