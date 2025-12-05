package cc.unitmesh.idea.context;

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightPlatformTestCase

class JavaClassContextBuilderTest: LightPlatformTestCase() {
    fun testShould_return_functions_from_kt_class_or_object() {
        val code = """
            package cc.unitmesh.untitled.demo.controller

            import org.springframework.web.bind.annotation.*
            
            @RestController
            @RequestMapping("/user")
            class UserController() {
                @GetMapping
                public UserDTO getUsers() {
                    return new UserDTO(1L, "username", "email", "name", "surname");
                }
            }
            """.trimIndent()

        val createFile =
            PsiFileFactory.getInstance(project).createFileFromText("UserController.java", JavaLanguage.INSTANCE, code)
        val controller = PsiTreeUtil.findChildOfType(createFile, PsiClass::class.java)!!

        // given
        val builder = JavaClassContextBuilder()

        // then
        val classContext = builder.getClassContext(controller, false)!!
        assertEquals(
            classContext.format(), "'package: cc.unitmesh.untitled.demo.controller.UserController\n" +
                    "'@RestController, @RequestMapping(\"/user\")\n" +
                    "class UserController {\n" +
                    "  \n" +
                    "  + @GetMapping     public UserDTO getUsers();\n" +
                    "}"        )
    }
}

