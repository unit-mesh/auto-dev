package cc.unitmesh.cpp.context;

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Ignore
import org.junit.Test

class CppMethodContextBuilderTest : BasePlatformTestCase() {
    fun testShouldGetFunctionNameOfCarsMethod() {
        // given
        val psiElement = myFixture.configureByText(
            "Car.cpp", """
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

//        val decl = PsiTreeUtil.getChildrenOfTypeAsList(psiElement, OCDeclaration::class.java).first()
//        val type = PsiTreeUtil.getChildrenOfTypeAsList(decl, OCTypeElement::class.java).first()
//        val clz = PsiTreeUtil.getChildrenOfTypeAsList(type, OCStructLike::class.java).first()
//        val function = PsiTreeUtil.getChildrenOfTypeAsList(clz, OCFunctionDeclaration::class.java).first()
//
//        val result = CppMethodContextBuilder()
//            .getMethodContext(function, false, true)!!
//
//        assertEquals("drive", result.name)
//        assertEquals("distance", result.paramNames.joinToString(", "))
//        assertEquals(
//            result.format(), """path: /src/Car.h
//language: c++
//fun name: drive
//fun signature: void drive(int distance)"""
//        )
    }
}
