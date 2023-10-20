package cc.unitmesh.idea.provider

import cc.unitmesh.idea.prompting.JvmPromptStrategy
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.LightPlatformTestCase


class JvmPromptStrategyTest : LightPlatformTestCase() {
    private val javaFactory: PsiElementFactory get() = JavaPsiFacade.getElementFactory(project)
    private val fileFactory: PsiFileFactory get() = PsiFileFactory.getInstance(project)
    private val originCode = """
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
    private val classCode = """
    package cc.unitmesh.untitled.demo.controller;

import cc.unitmesh.untitled.demo.dto.CreateBlogDto;
import cc.unitmesh.untitled.demo.entity.BlogPost;
import cc.unitmesh.untitled.demo.service.BlogService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class BlogController {
    $originCode
}
""".trimIndent()

    fun testShould_enable_get_service_code() {
        val advisor = JvmPromptStrategy()
        advisor.tokenLength = 90

        val psiClass = javaFactory.createClassFromText(originCode, null)
        psiClass.setName("HelloController")

        val usages = advisor.advice(psiClass, "BlogService")
        assertEquals(
            usages.prefixCode, """    @PostMapping("/blog")
    public BlogPost createBlog(CreateBlogDto blogDto) {
        BlogPost blogPost = new BlogPost();
        BeanUtils.copyProperties(blogDto, blogPost);
        return blogService.createBlog(blogPost);
    }

    @GetMapping("/blog")
    public List<BlogPost> getBlog() {
        return blogService.getAllBlogPosts();
    }"""
        )
    }

    fun testShould_enable_get_field_reference() {
        val advisor = JvmPromptStrategy()
        advisor.tokenLength = 30

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

    fun testShould_get_without_imports() {
        val advisor = JvmPromptStrategy()
        advisor.tokenLength = 120

        val psiFile = fileFactory.createFileFromText(JavaLanguage.INSTANCE, classCode)
        val usages = advisor.advice(psiFile as PsiJavaFile, "BlogService")
        assertEquals(
            usages.prefixCode, """@Controller
public class BlogController {
    
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

}"""
        )
    }
}