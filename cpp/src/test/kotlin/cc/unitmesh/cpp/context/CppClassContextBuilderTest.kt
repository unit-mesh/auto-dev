package cc.unitmesh.cpp.context;

import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.cidr.lang.OCFileType
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCStructLike
import com.jetbrains.cidr.lang.psi.OCTypeElement


class CppClassContextBuilderTest : LightPlatformTestCase() {
    fun testShouldGetFunctionNameOfCarsMethod() {
        // given
        val psiElement = PsiFileFactory.getInstance(project).createFileFromText(
            "Car.cpp", OCFileType.INSTANCE, """
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

//        // when
//        val result = CppStructContextBuilder().getClassContext(clz, true)!!
//
//        // then
//        assertEquals("Car", result.name)
//        assertEquals(1, result.methods.size)
//        assertEquals(3, result.fields.size)
    }
}
