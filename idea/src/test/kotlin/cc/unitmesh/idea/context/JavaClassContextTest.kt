package cc.unitmesh.idea.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.context.MethodContextProvider
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Ignore

class JavaClassContextTest : LightPlatformTestCase() {
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

    fun testShould_convert_class_to_string() {
        val psiFile = fileFactory.createFileFromText(JavaLanguage.INSTANCE, classCode)
        val psiElement = (psiFile as PsiJavaFile).classes[0]
        val classContext: ClassContext = ClassContextProvider(false).from(psiElement)

        assertEquals(
            classContext.toQuery(),
            """class name: BlogController
class fields: blogService
class methods: public BlogController(BlogService blogService)
@PostMapping("/blog")     public BlogPost createBlog(CreateBlogDto blogDto)
@GetMapping("/blog")     public List<BlogPost> getBlog()
super classes: []
"""
        )
    }

    fun testShould_convert_function_to_string() {
        val psiFile = fileFactory.createFileFromText(JavaLanguage.INSTANCE, classCode)
        val psiElement = (psiFile as PsiJavaFile).classes[0].methods[0]
        val context = MethodContextProvider(false, false).from(psiElement)

        assertEquals(
            context.toQuery(),
            """    fun name: BlogController
    fun language: Java
    fun signature: public BlogController(BlogService blogService)
    fun code: public BlogController(BlogService blogService) {
    this.blogService = blogService;
}"""
        )
    }
}