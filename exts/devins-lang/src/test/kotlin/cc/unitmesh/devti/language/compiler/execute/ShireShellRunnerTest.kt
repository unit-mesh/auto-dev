package cc.unitmesh.devti.language.compiler.execute

import cc.unitmesh.devti.language.processor.shell.ShireShellCommandRunner
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language

class ShireShellRunnerTest: BasePlatformTestCase() {
    fun testFill() {
        @Language("JSON")
        val jsonEnv = """
            {
              "development": {
                "name": "Phodal"
              }
            }
             """.trimIndent()

        myFixture.addFileToProject("demo.autodevEnv.json", jsonEnv)

        @Language("Shell Script")
        val content = """
            echo "Hello ${'$'}{name}, my name is ${'$'}{myName}!"
        """.trimIndent()

        val file = myFixture.addFileToProject("demo.seh", content)

        val fill = ShireShellCommandRunner.fill(
            project, file.virtualFile, mapOf(
                "myName" to "DevIn"
            )
        )

        assertEquals("echo \"Hello Phodal, my name is Shire!\"", fill)
    }
}
