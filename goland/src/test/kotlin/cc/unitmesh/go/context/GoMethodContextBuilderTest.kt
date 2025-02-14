package cc.unitmesh.go.context;

import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.psi.GoMethodDeclaration
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase

class GoMethodContextBuilderTest : BasePlatformTestCase() {
    fun testShouldHandleMethodSignature() {
        // given
        val file = myFixture.configureByText(
            "test.go", """
            package main

            func f3(float64, float64, float64) {}
            """.trimIndent()
        )

        val struct = PsiTreeUtil.getChildrenOfTypeAsList(file, GoFunctionOrMethodDeclaration::class.java).first()
        val context = GoMethodContextBuilder().getMethodContext(struct, false, false)

        TestCase.assertEquals(context?.name, "f3")
        TestCase.assertEquals(
            context?.format(), """
            path: /src/test.go
            language: Go
            fun name: f3
            fun signature: (float64, float64, float64)
            """.trimIndent()
        )
    }

    fun testShouldHandleGoMethodSignature() {
        val file = myFixture.configureByText(
            "test.go", """
            package main

            type T struct {
            	x int
            }

            func (pt *T) Double() {
            	pt.x *= 2
            }
            """.trimIndent()
        )

        val func = PsiTreeUtil.getChildrenOfAnyType(file, GoMethodDeclaration::class.java).first()
        val context = GoMethodContextBuilder().getMethodContext(func, false, false)

        TestCase.assertEquals(context?.name, "Double")
        TestCase.assertEquals(
            context?.format(), """
             path: /src/test.go
             language: Go
             fun name: Double
             fun signature: ()
             """.trimIndent()
        )
    }
}

