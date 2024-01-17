package cc.unitmesh.cpp.context;

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.cidr.lang.psi.*

class CppMethodContextBuilderTest : BasePlatformTestCase() {

    fun testShouldGetFunctionNameOfCarsMethod() {
        // given
        val psiElement = myFixture.configureByText(
            "Car.h", """
            #include <iostream>
            
            class Car {
            public:
                // class data
                std::string brand, model;
                int mileage = 0;
            
                void drive(int distance) {
                    mileage += distance;
                }
            };
            """.trimIndent()
        )

        val decl = PsiTreeUtil.getChildrenOfTypeAsList(psiElement, OCDeclaration::class.java).first()
        val type = PsiTreeUtil.getChildrenOfTypeAsList(decl, OCTypeElement::class.java).first()
        val clz = PsiTreeUtil.getChildrenOfTypeAsList(type, OCStructLike::class.java).first()
        val function = PsiTreeUtil.getChildrenOfTypeAsList(clz, OCFunctionDeclaration::class.java).first()
        println(function.text)

//        // when
//        val result = CppMethodContextBuilder()
//            .getMethodContext(function, true, true)!!
//
//        // then
//        assertEquals("drive", result.name)
//        assertEquals(1, result.paramNames)
//        assertEquals(result.format(), "")
    }
}
