package cc.unitmesh.cpp.context;

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CppClassContextBuilderTest : BasePlatformTestCase() {
    fun testShouldGetFunctionNameOfCarsMethod() {
        // given
        val psiElement = myFixture.configureByText(
            "car.cpp", """
            #include <iostream>
            
            class Car {
            public:
                // class data
                std::string brand, model;
                int mileage = 0;
            
                // class function
                void drive(int distance) {
                    mileage += distance;
                }
            };
            """.trimIndent()
        )

//        val decl = PsiTreeUtil.getChildrenOfTypeAsList(psiElement, OCDeclaration::class.java).first()
//        val type = PsiTreeUtil.getChildrenOfTypeAsList(decl, OCTypeElement::class.java).first()
//        val clz = PsiTreeUtil.getChildrenOfTypeAsList(type, OCStructLike::class.java).first()
//
//        // when
//        val result = CppStructContextBuilder().getClassContext(clz, true)!!
//
//        // then
//        assertEquals("Car", result.name)
//        assertEquals(1, result.methods.size)
//        assertEquals(3, result.fields.size)
    }
}
