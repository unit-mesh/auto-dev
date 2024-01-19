package cc.unitmesh.kotlin.context;

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightPlatformTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtPsiFactory

class KotlinClassContextBuilderTest : LightPlatformTestCase() {

    fun testShould_return_functions_from_kt_class_or_object() {
        val code = """
            package cc.unitmesh.untitled.demo.controller

            import org.springframework.web.bind.annotation.*
            
            @RestController
            @RequestMapping("/user")
            class UserController() {
                @GetMapping
                fun getUsers(): UserDTO {
                    return UserDTO(1L, "username", "email", "name", "surname")
                }
            }
            
            data class UserDTO(
                val id: Long? = null,
                val username: String,
                val email: String,
                val name: String,
                val surname: String? = null,
            )
            """.trimIndent()

        val createFile = KtPsiFactory(project).createFile("UserController.kt", code)
        val controller = PsiTreeUtil.findChildOfType(createFile, KtClassOrObject::class.java)!!
        // given
        val builder = KotlinClassContextBuilder()

        // then
        val classContext = builder.getClassContext(controller, false)!!
        TestCase.assertEquals(classContext.format(), "'package: cc.unitmesh.untitled.demo.controller.UserController\n" +
                "'@RestController, @RequestMapping(\"/user\")\n" +
                "class UserController {\n" +
                "  \n" +
                "  \n" +
                "}")
    }
}
