package cc.unitmesh.devti.analysis

import cc.unitmesh.devti.analysis.DtClass.Companion.fromPsiClass
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementFactory
import com.intellij.testFramework.LightPlatformTestCase

class DtClassTest: LightPlatformTestCase() {
    private val javaFactory: PsiElementFactory get() = JavaPsiFacade.getElementFactory(project)

    fun testShould_fetch_java_endpoints() {
        val originCode = """
    BlogService blogService;

    public HelloController(BlogService blogService) {
        this.blogService = blogService;
    }

    public String hello() {
        return "Greetings from Spring Boot!";
    }
            """.trimIndent()
        val psiClass = javaFactory.createClassFromText(originCode, null)
        psiClass.setName("HelloController")
        psiClass.addBefore(javaFactory.createAnnotationFromText("@Controller", null), psiClass.firstChild)

        val dtClass = DtClass.fromPsiClass(psiClass)
        assertEquals("""// package: HelloController
// class HelloController {
// blogService: BlogService
// + hello(): String
// }
// ' some other methods
""", dtClass.format())
    }
}
