package cc.unitmesh.devti.analysis

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import kotlin.math.log

class JavaCrudProcessorTest : BasePlatformTestCase() {
    private val javaFactory: PsiElementFactory get() = JavaPsiFacade.getElementFactory(project)

    fun testShould_create_a_new_controller() {
        val originCode = """""".trimIndent()
        val psiClass = javaFactory.createClassFromText(originCode, null)
        psiClass.setName("HelloController")
        psiClass.addBefore(javaFactory.createAnnotationFromText("@Controller", null), psiClass.firstChild)

        TestCase.assertEquals(psiClass.text, """@Controller class HelloController {

}""")
    }

    fun testShould_create_new_file() {
        val newController = PsiFileFactory.getInstance(project).createFileFromText(
            "HelloController.java", JavaLanguage.INSTANCE, """@Controller class HelloController {
            |
            |}""".trimMargin()
        )

//        val psiDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(myFixture.tempDirFixture.findOrCreateDir("src/main/java"))
//        println(psiDirectory)
//        val newFile = psiDirectory.add(newController)
    }
}