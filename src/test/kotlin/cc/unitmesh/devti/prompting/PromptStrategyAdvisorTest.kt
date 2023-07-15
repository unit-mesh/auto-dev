package cc.unitmesh.devti.prompting

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementFactory
import com.intellij.testFramework.LightPlatformTestCase


class PromptStrategyAdvisorTest : LightPlatformTestCase() {
    private val javaFactory: PsiElementFactory get() = JavaPsiFacade.getElementFactory(project)

    fun testShould_enable_get_field_reference() {
        val advisor = PromptStrategyAdvisor(project)
        advisor.tokenLength = 30

        val originCode = """
    BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }
    
    @PostMapping("/blog")
    public BlogPost createBlog(CreateBlogDto blogDto) {
        BlogPost blogPost = new BlogPost();
        BeanUtils.copyProperties(blogDto, blogPost);
        return blogService.createBlog(blogPost);
    }

    @GetMapping("/blog")
    public List<BlogPost> getBlog() {
        return blogService.getAllBlogPosts();
    }
"""
        val psiClass = javaFactory.createClassFromText(originCode, null)
        psiClass.setName("HelloController")

        val usages = advisor.advice(psiClass, "BlogService")
        assertEquals(
            usages.prefixCode, """        {some other code}
        return blogService.createBlog(blogPost);
        {some other code}
        return blogService.getAllBlogPosts();
"""
        )
    }
}