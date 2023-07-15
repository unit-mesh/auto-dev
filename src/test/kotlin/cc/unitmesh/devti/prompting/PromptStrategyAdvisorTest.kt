package cc.unitmesh.devti.prompting

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementFactory
import com.intellij.testFramework.LightPlatformTestCase


class PromptStrategyAdvisorTest: LightPlatformTestCase() {
    private val javaFactory: PsiElementFactory get() = JavaPsiFacade.getElementFactory(project)

    fun testShould_enable_get_field_reference() {
        val advisor = PromptStrategyAdvisor(project)
        advisor.tokenLength = 30

        val originCode = """
    BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }
"""
        val psiClass = javaFactory.createClassFromText(originCode, null)
        psiClass.setName("HelloController")


        advisor.advice(psiClass, "")
    }
}