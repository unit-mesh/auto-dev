package cc.unitmesh.go.context

import com.goide.psi.GoTypeDeclaration
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GoStructContextBuilderTest : BasePlatformTestCase() {
    fun testGetClassContext() {
        val file = myFixture.configureByText(
            "test.go", """
            package main

            type Test struct {
                Name string
            }

            func (t *Test) GetName() string {
                return t.Name
            }
        """.trimIndent()
        )

        val struct = PsiTreeUtil.getChildrenOfTypeAsList(file, GoTypeDeclaration::class.java).first()
        val context = GoStructContextBuilder().getClassContext(struct, false)

        assertEquals("Test", context?.name)
        assertEquals(1, context?.methods?.size)
    }
}
