package cc.unitmesh.kotlin.provider;

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory


class KotlinTestDataBuilderTest : LightPlatformTestCase() {
    private val code = """
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

    fun testShouldPass() {
        assertTrue(true)
    }

    fun testShouldParseBaseRoute() {
        val createFile = KtPsiFactory(project).createFile("UserController.kt", code)
        val builder = KotlinTestDataBuilder()

        val firstFunction = PsiTreeUtil.findChildOfType(createFile, KtNamedFunction::class.java)!!

        assertEquals(builder.baseRoute(firstFunction), "/user")
    }

    // test will fail if 222
    fun shouldReturnLangFileSuffix() {
        val createFile = KtPsiFactory(project).createFile("UserController.kt", code)
        val builder = KotlinTestDataBuilder()

        val firstFunction = PsiTreeUtil.findChildOfType(createFile, KtNamedFunction::class.java)!!
        val outboundData = builder.outboundData(firstFunction)

        assertEquals(outboundData.size, 1)
        assertEquals(
            outboundData["cc.unitmesh.untitled.demo.controller.UserDTO"],
            "'package: cc.unitmesh.untitled.demo.controller.UserDTO\n" +
                    "class UserDTO {\n" +
                    "  \n" +
                    "  \n" +
                    "}"
        )
    }
}
