package cc.unitmesh.devti.language.compiler

import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.konan.file.File

class DevInCompilerTest : LightJavaCodeInsightFixtureTestCase() {
    fun testNormalString() {
        @Language("DevIn")
        val code = "Normal String"
        val file = myFixture.configureByText("test.devin", code)

        val compile = DevInsCompiler(project, file as DevInFile, myFixture.editor).compile()
        assertEquals("Normal String", compile.output)
    }

    fun testForWriting() {
        val projectPath = project.basePath + File.separator

        @Language("DevIn")
        val code = "/write:${projectPath}Sample.devin#L1-L2\n```devin\nNormal String /\n```"
        myFixture.configureByText("Sample.devin", "Sample Text")
        val file = myFixture.configureByText("test.devin", code)

        try {
            val compile = DevInsCompiler(project, file as DevInFile, myFixture.editor).compile()
            println(compile.output)
        } catch (e: Exception) {
//            fail(e.message)
        }
    }

    fun testForRefactoring() {
        @Language("DevIn")
        val code = "/refactor:rename cc.unitmesh.devti.language.run.DevInsProgramRunner to cc.unitmesh.devti.language.run.DevInsProgramRunnerImpl"
        val file = myFixture.configureByText("test.devin", code)

        try {
            val compile = DevInsCompiler(project, file as DevInFile, myFixture.editor).compile()
            println(compile.output)
        } catch (e: Exception) {
//            fail(e.message)
        }
    }
}

