package cc.unitmesh.idea.java

import cc.unitmesh.idea.flow.JavaCodeProcessor
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.LightPlatformTestCase
import junit.framework.TestCase

class JavaCodeProcessorTest : LightPlatformTestCase() {
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

    fun testShould_filter_called_service_line_inside_controller() {
        val code = """
            package cc.unitmesh.devti.controller;
            
            import cc.unitmesh.devti.service.UserService;
            import cc.unitmesh.devti.service.UserServiceImpl;
            
            public class UserController {
                private UserService userService = new UserServiceImpl();
                
                public void getUser() {
                    userService.getUser();
                }
            }
        """.trimIndent()

        val usedMethodCode = JavaCodeProcessor.findUsageCode(code, "UserService")
        assert(usedMethodCode.size == 1)

        assertEquals("userService.getUser();", usedMethodCode[0])
    }

    fun testShould_filter_called_service_line_with_parameters_inside_controller() {
        val code = """
            package cc.unitmesh.devti.controller;
            
            import cc.unitmesh.devti.service.UserService;
            import cc.unitmesh.devti.service.UserServiceImpl;
            
            public class UserController {
                private UserService userService = new UserServiceImpl();
                
                public void getUser() {
                    userService.getUser("1");
                }
            }
        """.trimIndent()

        val usedMethodCode = JavaCodeProcessor.findUsageCode(code, "UserService")
        assert(usedMethodCode.size == 1)

        assertEquals("userService.getUser(\"1\");", usedMethodCode[0])
    }

    fun testShould_find_no_exist_methods() {
        val psiFile = fileFactory.createFileFromText(JavaLanguage.INSTANCE, classCode)
        val findNoExistMethod =
            JavaCodeProcessor.findNoExistMethod(psiFile as PsiJavaFile, listOf("blogService.deleteBlogPost();", "blogService.createBlog(blogPost)"))

        TestCase.assertEquals(findNoExistMethod.size, 1)
        assertEquals("deleteBlogPost", findNoExistMethod[0])
    }
}