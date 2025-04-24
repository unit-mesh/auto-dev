package cc.unitmesh.devti.language.regression

import cc.unitmesh.devti.language.ast.variable.ShireVariableTemplateCompiler
import cc.unitmesh.devti.language.compiler.DevInsCompiler
import cc.unitmesh.devti.language.middleware.post.PostProcessorContext
import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import kotlin.text.get

class ShireCompileTest : BasePlatformTestCase() {
    val javaHelloController = """
            package com.phodal.shirelang.controller;
            
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;
            
            @RestController
            public class HelloController {
                @GetMapping("/hello")
                public String hello() {
                    return "Hello, World!";
                }
            }
        """.trimIndent()

    val javaHelloEntity = """
            package com.phodal.shirelang.entity;
            
            public class HelloEntity {
                private String name;
            
                public String getName() {
                    return name;
                }
            
                public void setName(String name) {
                    this.name = name;
                }
            }
        """.trimIndent()

    fun testShouldReturnControllerCodeWithFindCat() {
        myFixture.addFileToProject(
            "src/main/java/com/phodal/shirelang/controller/HelloController.java",
            javaHelloController
        )
        myFixture.addFileToProject("src/main/java/com/phodal/shirelang/entity/HelloEntity.java", javaHelloEntity)

        @Language("Shire")
        val code = """
            ---
            name: "类图分析"
            variables:
              "controllers": /.*.java/ { find("Controller") | grep("src/main/java/.*")  | cat }
              "outputFile": /any/ { print("name.adl") }
            onStreamingEnd: { parseCode | saveFile(${'$'}outputFile) }
            ---
            
            
            请将下列信息原样输出，不要添加任何其他描述信息：
            
            ${'$'}controllers
        """.trimIndent()

        val file = myFixture.configureByText("test.devin", code)
        val compile = runBlocking { DevInsCompiler(project, file as DevInFile, myFixture.editor).compile() }
        val hole = compile.config!!

        val context = PostProcessorContext(
            genText = "User prompt:\n\n",
        )

        runBlocking {
            val templateCompiler =
                ShireVariableTemplateCompiler(project, hole, compile.variableTable, code, myFixture.editor)
            val compiledVariables =
                templateCompiler.compileVariable(myFixture.editor, mutableMapOf())

            context.compiledVariables = compiledVariables
        }

        assertEquals(
            """package com.phodal.shirelang.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }
}""", context.compiledVariables["controllers"]
        )
    }

    fun testShouldReturnControllerCodeWithFindCatWithHead() {
        myFixture.addFileToProject(
            "src/main/java/com/phodal/shirelang/controller/HelloController.java",
            javaHelloController
        )
        myFixture.addFileToProject("src/main/java/com/phodal/shirelang/entity/HelloEntity.java", javaHelloEntity)

        @Language("DevIn")
        val code = """
            ---
            name: "类图分析"
            variables:
              "output": "name.adl"
              "con": /.*.java/ { print | head(1)}
              "controllers": /.*.java/ { find("Controller") | grep("src/main/java/.*") | head(1)  | cat   }
              "outputFile": /any/ { print("name.adl") }
            onStreamingEnd: { parseCode | saveFile(${'$'}outputFile) }
            
            ---
            
            下面是你要执行转换的数据：
            ${'$'}controllers
        """.trimIndent()

        val file = myFixture.configureByText("test.devin", code)
        val compile = runBlocking { DevInsCompiler(project, file as DevInFile, myFixture.editor).compile() }
        val hole = compile.config!!

        val context = PostProcessorContext(
            genText = "User prompt:\n\n",
        )

        runBlocking {
            val templateCompiler = ShireVariableTemplateCompiler(project, hole, compile.variableTable, code, myFixture.editor)
            val compiledVariables =
                templateCompiler.compileVariable(myFixture.editor, mutableMapOf())

            context.compiledVariables = compiledVariables
        }

        assertEquals(
            """/src/src/main/java/com/phodal/shirelang/entity/HelloEntity.java""",
            context.compiledVariables["con"]
        )
        assertEquals("package com.phodal.shirelang.controller;\n" +
                "\n" +
                "import org.springframework.web.bind.annotation.GetMapping;\n" +
                "import org.springframework.web.bind.annotation.RestController;\n" +
                "\n" +
                "@RestController\n" +
                "public class HelloController {\n" +
                "    @GetMapping(\"/hello\")\n" +
                "    public String hello() {\n" +
                "        return \"Hello, World!\";\n" +
                "    }\n" +
                "}", context.compiledVariables["controllers"])
    }

//    fun testShouldCompileMarkdownHeader() {
//
//        @Language("Shire")
//        val code = """
//            ## Header
//
//            Body
//        """.trimIndent()
//
//        val p: Properties = Properties()
//        p.setProperty("resource.loader", "class")
//        p.setProperty("class.resource.loader.description", "Velocity Classpath Resource Loader")
//        p.setProperty(
//            "class.resource.loader.class",
//            "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader"
//        )
//        try {
//            Velocity.init(p);
//        } catch (e: Exception) {
////            e.printStackTrace()
//        }
//
//        val file = myFixture.configureByText("test.devin", code)
//        val compile = ShireSyntaxAnalyzer(project, file as DevInFile, myFixture.editor).parse()
//
//        val result = runBlocking {
//            val templateCompiler = ShireTemplateCompiler(project, compile.config, compile.variableTable, code)
//            templateCompiler.compile()
//        }
//
//        assertEquals("", result)
//    }
}
