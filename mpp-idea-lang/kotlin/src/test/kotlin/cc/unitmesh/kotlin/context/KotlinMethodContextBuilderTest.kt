package cc.unitmesh.kotlin.context;

import cc.unitmesh.kotlin.util.KotlinPsiUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightPlatformTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory


class KotlinMethodContextBuilderTest : LightPlatformTestCase() {
    fun testShouldIgnoreMethodComments() {
        // given
        val code = """
            /**
             * It's a hello, world.
             */
            fun main() {
                println("Hello, World!")
            }
            """.trimIndent()

        val createFile = KtPsiFactory(project).createFile("UserController.kt", code)
        val clz = PsiTreeUtil.findChildOfType(createFile, KtNamedFunction::class.java)!!

        val signatureString = KotlinPsiUtil.signatureString(clz)
        TestCase.assertEquals(signatureString, "/**  * It's a hello, world.  */ fun main()")
    }

    fun testShouldIgnoreClassFunctionComment() {
        // given
        val code = """
            class UserController {
                /**
                 * It's a hello, world.
                 */
                fun main() {
                    println("Hello, World!")
                }
            }
            """.trimIndent()

        val createFile = KtPsiFactory(project).createFile("UserController.kt", code)
        val clz = PsiTreeUtil.findChildOfType(createFile, KtNamedFunction::class.java)!!

        val signatureString = KotlinPsiUtil.signatureString(clz)
        TestCase.assertEquals(signatureString, "/**      * It's a hello, world.      */     fun main()")
    }

    fun testShouldHandleNormalClassFunctionComment() {
        // given
        val code = """
            class UserController {
                fun main() {
                    println("Hello, World!")
                }
            }
            """.trimIndent()

        val createFile = KtPsiFactory(project).createFile("UserController.kt", code)
        val clz = PsiTreeUtil.findChildOfType(createFile, KtNamedFunction::class.java)!!

        val signatureString = KotlinPsiUtil.signatureString(clz)
        TestCase.assertEquals(signatureString, "fun main()")
    }
}
