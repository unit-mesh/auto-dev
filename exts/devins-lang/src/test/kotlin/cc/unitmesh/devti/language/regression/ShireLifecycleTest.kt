package cc.unitmesh.devti.language.regression

import cc.unitmesh.devti.language.DevInLanguage
import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.devins.post.LifecycleProcessorSignature
import cc.unitmesh.devti.devins.post.PostProcessor.Companion.handler
import cc.unitmesh.devti.devins.post.PostProcessorContext
import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.TestOnly

class ShireLifecycleTest : BasePlatformTestCase() {
    fun testShouldHandleWhenStreamingEnd() {
        @Language("DevIn")
        val code = """
            ---
            onStreamingEnd:  { parseCode | saveFile("demo.devin") | verifyCode | runCode }
            ---
            
            ${'$'}allController
        """.trimIndent()

        val file = myFixture.addFileToProject("sample.devin", code)

        myFixture.openFileInEditor(file.virtualFile)

        val compile = runBlocking { DevInsCompiler(project, file as DevInFile, myFixture.editor).compile() }
        val hole = compile.config!!

        val funcNode = hole.onStreamingEnd

        assertEquals(funcNode.size, 4)
        assertEquals(funcNode[0].funcName, "parseCode")
        assertEquals(funcNode[0].args.size, 0)

        assertEquals(funcNode[1].funcName, "saveFile")
        assertEquals(funcNode[1].args[0], "demo.devin")

        assertEquals(funcNode[2].funcName, "verifyCode")
        assertEquals(funcNode[3].funcName, "runCode")

        try {
            val handleContext = PostProcessorContext(currentLanguage = DevInLanguage.INSTANCE, editor = null)
            execute(project, funcNode, handleContext, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @TestOnly
    fun execute(
        project: Project,
        funcNodes: List<LifecycleProcessorSignature>,
        handleContext: PostProcessorContext,
        console: ConsoleView?,
    ) {
        funcNodes.forEach { funNode ->
            val handler = handler(funNode.funcName)
            if (handler != null) {
                handler.setup(handleContext)
                handler.execute(project, handleContext, console, funNode.args)
                handler.finish(handleContext)
            }
        }
    }

    fun testShouldHandleWhenAfterStreaming() {
        @Language("DevIn")
        val code = """
            ---
            afterStreaming: {
                condition {
                  "error"       { output.length < 1 }
                  "success"     { output.length > 1 }
                  "json-result" { jsonpath("${'$'}.store.*") }
                }
                case condition {
                  "error"       { notify("Failed to Generate JSON") }
                  "success"     { notify("Success to Generate JSON") }
                  "json-result" { execute("sample2.devin") }
                  default       { notify("Failed to Generate JSON") /* mean nothing */ }
                }
              }
            ---
            
            ${'$'}allController
        """.trimIndent()

        val file = myFixture.addFileToProject("sample.devin", code)

        myFixture.openFileInEditor(file.virtualFile)

        val compile = runBlocking { DevInsCompiler(project, file as DevInFile, myFixture.editor).compile() }
        val hole = compile.config!!

        val funcNode = hole.afterStreaming!!

        TestCase.assertEquals(funcNode.conditions.size, 3)
        TestCase.assertEquals(funcNode.conditions[0].conditionKey, "\"error\"")

        assertEquals(funcNode.conditions[2].valueExpression.display(), "jsonpath(\"${'$'}.store.*\")")

        TestCase.assertEquals(funcNode.cases.size, 4)
        TestCase.assertEquals(funcNode.cases[0].caseKey, "\"error\"")

        val genJson = """
            {
                "store": {
                    "book": [
                        {
                            "category": "reference",
                            "author": "Nigel Rees",
                            "title": "Sayings of the Century",
                            "price": 8.95
                        }
                    ]
                }
            }
           """.trimIndent()
        val handleContext = PostProcessorContext(
            currentLanguage = DevInLanguage.INSTANCE,
            genText = genJson,
            editor = null
        )

        assertThrows(RuntimeException::class.java) {
            hole.afterStreaming?.execute(myFixture.project, handleContext, hole)
        }
    }

    fun testShouldSupportForBeforeStreaming() {
        @Language("DevIn")
        val code = """
            ---
            beforeStreaming: { caching("disk") | splitting | embedding }
            ---
            
            ${'$'}allController
        """.trimIndent()

        val file = myFixture.addFileToProject("sample.devin", code)

        myFixture.openFileInEditor(file.virtualFile)

        val compile = runBlocking { DevInsCompiler(project, file as DevInFile, myFixture.editor).compile() }
        val hole = compile.config!!.beforeStreaming!!

        assertEquals(hole.processors.size, 3)
        assertEquals(hole.processors[0].funcName, "caching")
        assertEquals(hole.processors[1].funcName, "splitting")
        assertEquals(hole.processors[2].funcName, "embedding")
    }

    fun testShouldExecuteProcessForOnStreamingEvent() {
        @Language("DevIn")
        val code = """
            ---
            onStreaming: { logging() }
            ---
            
            ${'$'}allController
        """.trimIndent()

        val file = myFixture.addFileToProject("sample.devin", code)

        myFixture.openFileInEditor(file.virtualFile)

        val compile = runBlocking { DevInsCompiler(project, file as DevInFile, myFixture.editor).compile() }
        val funcSigns = compile.config!!.onStreaming

        assertEquals(funcSigns.size, 1)
        assertEquals(funcSigns[0].funcName, "logging")
    }
}