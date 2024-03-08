package cc.unitmesh.scala.context

import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.plugins.scala.ScalaFileType

class ScalaClassContextBuilderTest : LightPlatformTestCase() {
    fun testShouldGetClassName() {
        val fileFactory: PsiFileFactory = PsiFileFactory.getInstance(project)

        val classCode = """
            class Point(var x: Int, var y: Int):
              def move(dx: Int, dy: Int): Unit =
                x = x + dx
                y = y + dy
            
              override def toString: String =
                s"(${'$'}x, ${'$'}y)"
            end Point
            """.trimIndent()

        val psiFile = fileFactory.createFileFromText("Point.scala", ScalaFileType.INSTANCE, classCode)
        val psiElement = psiFile.children[0]
        val classContext = ScalaClassContextBuilder().getClassContext(psiElement, false)!!

        assertEquals(classContext.name, "Point")
    }
}