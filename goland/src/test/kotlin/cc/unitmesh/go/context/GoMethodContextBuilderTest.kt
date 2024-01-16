package cc.unitmesh.go.context;

import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.psi.GoTypeDeclaration
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

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

        assertEquals(context?.name, "f3")
        assertEquals(context?.format(), """
            path: /src/test.go
            language: Go
            fun name: f3
            fun signature: (float64, float64, float64)
            """.trimIndent())
    }
}
