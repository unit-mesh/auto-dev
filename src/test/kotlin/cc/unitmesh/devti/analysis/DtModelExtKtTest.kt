package cc.unitmesh.devti.analysis

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementFactory
import com.intellij.testFramework.LightPlatformTestCase

class DtModelExtKtTest: LightPlatformTestCase() {
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
        assertEquals("""class HelloController constructor(blogService: PsiType:BlogService)
- methods: hello(): PsiType:String""", dtClass.format())
    }
}
