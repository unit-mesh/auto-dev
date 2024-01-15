package cc.unitmesh.csharp.context;

import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightPlatform4TestCase
import com.jetbrains.rider.ideaInterop.fileTypes.csharp.CSharpFileType
import org.junit.Test

class CsharpClassContextBuilderTest : LightPlatform4TestCase() {

    @Test
    fun testShouldReturnNullWhenPsiElementIsNotCSharpBlock() {
        val code = """
            // Hello World! program
            namespace HelloWorld
            {
                class Hello {         
                    static void Main(string[] args)
                    {
                        System.Console.WriteLine("Hello World!");
                    }
                }
            }
            """.trimIndent()

//        val psiElement =
//            PsiFileFactory.getInstance(project)
//                .createFileFromText("HelloWorld.cs", CSharpFileType, code)

//        val builder: ClassContextBuilder = CsharpClassContextBuilder()
//        builder.getClassContext(psiElement, false)
    }
}
