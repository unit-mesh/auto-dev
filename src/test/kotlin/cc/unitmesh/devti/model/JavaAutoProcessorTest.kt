package cc.unitmesh.devti.analysis

import cc.unitmesh.devti.flow.code.JavaSpringCodeCreator
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementFactory
import com.intellij.testFramework.LightPlatformTestCase

class JavaAutoProcessorTest : LightPlatformTestCase() {
    private val javaFactory: PsiElementFactory get() = JavaPsiFacade.getElementFactory(project)

    fun testShould_fetch_java_endpoints() {
        val originCode = """
    public String hello() {
        return "Greetings from Spring Boot!";
    }
            """.trimIndent()
        val psiClass = javaFactory.createClassFromText(originCode, null)
        psiClass.setName("HelloController")
        psiClass.addBefore(javaFactory.createAnnotationFromText("@Controller", null), psiClass.firstChild)

        val endpoints = JavaSpringCodeCreator(project)
        val newClasses =
            endpoints.addMethodToClass(psiClass, """
    public String hello2() { 
        return "Greetings from Spring Boot!"; 
    }
""".trimIndent())

        assertEquals("hello", newClasses.methods[0].name)
        assertEquals("hello2", newClasses.methods[1].name)
    }
}