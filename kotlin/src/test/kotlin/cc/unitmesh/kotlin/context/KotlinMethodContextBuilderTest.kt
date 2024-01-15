package cc.unitmesh.kotlin.context;

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightPlatformTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.junit.Test


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

        val signatureString = KotlinMethodContextBuilder.Util.getSignatureString(clz)
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

        val signatureString = KotlinMethodContextBuilder.Util.getSignatureString(clz)
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

        val signatureString = KotlinMethodContextBuilder.Util.getSignatureString(clz)
        TestCase.assertEquals(signatureString, "fun main()")
    }
}
