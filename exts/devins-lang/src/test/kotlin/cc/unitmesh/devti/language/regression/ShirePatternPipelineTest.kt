package cc.unitmesh.devti.language.regression

import cc.unitmesh.devti.language.ast.action.PatternActionProcessor
import cc.unitmesh.devti.language.ast.variable.ShireVariableTemplateCompiler
import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.language.middleware.post.PostProcessorContext
import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language

class ShirePatternPipelineTest : BasePlatformTestCase() {
    fun testShouldSupportForTee() {
        @Language("DevIn")
        val code = """
            ---
            name: Summary
            description: "Generate Summary"
            interaction: AppendCursor
            data: ["a", "b"]
            when: ${'$'}fileName.matches("/.*.java/")
            variables:
              "var2": /.*ple.devin/ { cat | find("fileName") | sort }
            onStreamingEnd: { append(${'$'}var2) | saveFile("summary.md") }
            ---
            
            Summary webpage: ${'$'}fileName
        """.trimIndent()

        val file = myFixture.addFileToProject("sample.devin", code)

        myFixture.openFileInEditor(file.virtualFile)

        val compile = runBlocking { DevInsCompiler(project, file as DevInFile, myFixture.editor).compile() }
        val hole = compile.config!!

        val context = PostProcessorContext(
            genText = "User prompt:\n\n",
        )

        runBlocking {
            val shireTemplateCompiler =
                ShireVariableTemplateCompiler(project, hole, compile.variableTable, code, myFixture.editor)
            val compiledVariables =
                shireTemplateCompiler.compileVariable(myFixture.editor, mutableMapOf())

            context.compiledVariables = compiledVariables

            hole.variables.mapValues {
                PatternActionProcessor(project, hole, mutableMapOf()).execute(it.value)
            }

            hole.setupStreamingEndProcessor(project, context = context)
            hole.executeStreamingEndProcessor(project, null, context = context, compiledVariables)
        }

        assertEquals("User prompt:\n\n" +
                "  \"var2\": /.*ple.devin/ { cat | find(\"fileName\") | sort }\n" +
                "Summary webpage: \$fileName\n" +
                "when: \$fileName.matches(\"/.*.java/\")", context.genText)
    }

    fun testShouldSupportAfterStreamingPattern() {
        @Language("DevIn")
        val code = """
            ---
            name: Summary
            description: "Generate Summary"
            interaction: AppendCursor
            variables:
              "var2": "sample"
            afterStreaming: { 
                case condition {
                  default { print(${'$'}output) }
                }
            }
            ---
            
            Summary webpage: ${'$'}fileName
        """.trimIndent()

        val file = myFixture.addFileToProject("sample.devin", code)

        myFixture.openFileInEditor(file.virtualFile)

        val compile = runBlocking { DevInsCompiler(project, file as DevInFile, myFixture.editor).compile() }
        val hole = compile.config!!

        val context = PostProcessorContext(
            genText = "User prompt:\n\n",
        )

        runBlocking {
            val shireTemplateCompiler = ShireVariableTemplateCompiler(project, hole, compile.variableTable, code, myFixture.editor)
            val compiledVariables =
                shireTemplateCompiler.compileVariable(myFixture.editor, mutableMapOf())

            context.compiledVariables = compiledVariables

            hole.variables.mapValues {
                PatternActionProcessor(project, hole, mutableMapOf()).execute(it.value)
            }

            hole.setupStreamingEndProcessor(project, context = context)
            hole.executeAfterStreamingProcessor(project, null, context = context)
        }

        assertEquals("User prompt:\n\n", context.lastTaskOutput)
    }

    fun testShouldUseSedReplaceContentInVariables() {
        @Language("DevIn")
        val code = """
            ---
            name: Summary
            description: "Generate Summary"
            interaction: AppendCursor
            variables:
              "openai": "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"
              "var2": /.*ple.devin/ { cat | find("openai") | sed("(?i)\b(sk-[a-zA-Z0-9]{20}T3BlbkFJ[a-zA-Z0-9]{20})(?:['|\"|\n|\r|\s|\x60|;]|${'$'})", "sk-***") }
            ---
            
            Summary webpage: ${'$'}var2
        """.trimIndent()

        val file = myFixture.addFileToProject("sample.devin", code)

        myFixture.openFileInEditor(file.virtualFile)

        val compile = runBlocking { DevInsCompiler(project, file as DevInFile, myFixture.editor).compile() }
        val hole = compile.config!!

        val context = PostProcessorContext(
            genText = "User prompt:\n\n",
        )

        runBlocking {
            val shireTemplateCompiler = ShireVariableTemplateCompiler(project, hole, compile.variableTable, code, myFixture.editor)
            val compiledVariables =
                shireTemplateCompiler.compileVariable(myFixture.editor, mutableMapOf())

            context.compiledVariables = compiledVariables

            hole.variables.mapValues {
                PatternActionProcessor(project, hole, mutableMapOf()).execute(it.value)
            }

            hole.setupStreamingEndProcessor(project, context = context)
            hole.executeAfterStreamingProcessor(project, null, context = context)
        }

        assertEquals("  \"openai\": \"sk-***\n" +
                "  \"var2\": /.*ple.devin/ { cat | find(\"openai\") | sed(\"(?i)\\b(sk-[a-zA-Z0-9]{20}T3BlbkFJ[a-zA-Z0-9]{20})(?:['|\\\"|\\n|\\r|\\s|\\x60|;]|\$)\", \"sk-***\") }", context.compiledVariables["var2"]
        )
    }
}
