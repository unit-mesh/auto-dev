// for test intellij plugin
import com.intellij.testFramework.LightPlatformTestCase
class /*TestClassName*/Test : LightPlatformTestCase() {
    // the Intellij test should start with test
    fun test/* with should_xx_given_xx_when_xxx */() {
        // the code to test

        // create mock code if needed
        val code = """
            /**
             * It's a hello, world.
             */
            fun main() {
                println("Hello, World!")
            }
            """.trimIndent()

        val createFile = KtPsiFactory(project).createFile("Main.kt", code)
        val clz = PsiTreeUtil.findChildOfType(createFile, KtNamedFunction::class.java)!!
    }
}