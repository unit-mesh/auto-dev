package cc.unitmesh.devti.java.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightPlatformTestCase
import junit.framework.TestCase

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
        val classContext: ClassContext = ClassContextProvider(false).from(psiFile)

        TestCase.assertEquals(classContext.toQuery(), "")
    }
}