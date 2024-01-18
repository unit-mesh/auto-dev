package cc.unitmesh.cpp.util;

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.psi.OCStructLike
import com.jetbrains.cidr.lang.psi.OCTypeElement

class CppContextPrettifyTest : BasePlatformTestCase() {

    fun testShouldExtractStructureText() {
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
//        val struct = PsiTreeUtil.getChildrenOfTypeAsList(type, OCStructLike::class.java).first()
//
//        // when
//        val result = CppContextPrettify.printStructure(struct as OCStruct)
//
//        // then
//        val expected = """
//        struct Car {
//        // class data
//            std::string brand, model;
//        int mileage = 0;
//        // class function
//        void drive(int distance);
//        }
//        """.trimIndent()
//
//        assertEquals(expected, result)
    }
}
