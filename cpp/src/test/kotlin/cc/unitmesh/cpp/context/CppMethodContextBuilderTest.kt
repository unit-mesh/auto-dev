package cc.unitmesh.cpp.context;

import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.cidr.lang.OCFileType
import com.jetbrains.cidr.lang.psi.OCDeclaration
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.cidr.lang.psi.OCStructLike
import com.jetbrains.cidr.lang.psi.OCTypeElement


class CppMethodContextBuilderTest : LightPlatformTestCase() {
    fun testShouldGetFunctionNameOfCarsMethod() {

        // given
        val psiElement = PsiFileFactory.getInstance(project).createFileFromText(
            "Car.cpp", OCFileType.INSTANCE,  """
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
//            result.format(), """path: _
//language: c++
//fun name: drive
//fun signature: void drive(int distance)"""
//        )
    }
}
