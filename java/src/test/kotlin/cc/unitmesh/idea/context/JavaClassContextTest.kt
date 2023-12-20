package cc.unitmesh.idea.context

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.context.ClassContextProvider
import cc.unitmesh.devti.context.MethodContextProvider
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class JavaClassContextTest : LightJavaCodeInsightFixtureTestCase() {
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
    private val controllerCode = """
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

    private val serviceCode: String = """
package cc.unitmesh.untitled.demo.service;

import cc.unitmesh.untitled.demo.entity.BlogPost;
import cc.unitmesh.untitled.demo.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BlogService {
    @Autowired
    BlogRepository blogRepository;

    public BlogPost createBlog(BlogPost blogDto) {
        return blogRepository.save(blogDto);
    }

    public BlogPost getBlogById(Long id) {
        return blogRepository.findById(id).orElse(null);
    }

    public BlogPost updateBlog(Long id, BlogPost blogDto) {
        return blogRepository.findById(id).map(blog -> {
            blog.setTitle(blogDto.getTitle());
            blog.setContent(blogDto.getContent());
            return blogRepository.save(blog);
        }).orElse(null);
    }

    public void deleteBlog(Long id) {
        blogRepository.deleteById(id);
    }
}
"""

    fun testShould_convert_class_to_string() {
        val psiFile = fileFactory.createFileFromText(JavaLanguage.INSTANCE, controllerCode)
        val psiElement = (psiFile as PsiJavaFile).classes[0]
        val classContext: ClassContext = ClassContextProvider(false).from(psiElement)

        assertEquals(
            classContext.format(),
            """'package: cc.unitmesh.untitled.demo.controller.BlogController
class BlogController {
  blogService
  + public BlogController(BlogService blogService)
  + @PostMapping("/blog")     public BlogPost createBlog(CreateBlogDto blogDto)
  + @GetMapping("/blog")     public List<BlogPost> getBlog()
}"""
        )
    }

    fun testShould_convert_function_to_string() {
        myFixture.addClass(serviceCode)
        myFixture.addClass(controllerCode)

        val serviceFile = myFixture.findClass("cc.unitmesh.untitled.demo.service.BlogService")
        val psiElement = serviceFile.methods[0]
        val context = MethodContextProvider(false, true).from(psiElement)

        assertEquals(
            context.format(),
            """
               |language: Java
               |fun name: createBlog
               |fun signature: public BlogPost createBlog(BlogPost blogDto)
               |usages: 
               |BlogController.java -> blogService.createBlog""".trimMargin()
        )
    }
}
