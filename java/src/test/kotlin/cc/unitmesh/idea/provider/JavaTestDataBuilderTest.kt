package cc.unitmesh.idea.provider;

import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class JavaTestDataBuilderTest : LightJavaCodeInsightFixtureTestCase() {
    fun testBaseRoute() {
//        val springController = myFixture.addFileToProject(
//            "src/main/java/com/example/SpringController.java",
//            """
//            package com.example;
//
//            import org.springframework.web.bind.annotation.RequestMapping;
//            import org.springframework.web.bind.annotation.RestController;
//
//            @RestController
//            @RequestMapping("/api")
//            public class SpringController {
//                @RequestMapping("/hello")
//                public String hello() {
//                    return "Hello World!";
//                }
//            }
//            """.trimIndent()
//        )
//
//        val method = PsiTreeUtil.findChildOfType(springController, PsiMethod::class.java)
//        val baseRoute = JavaPsiElementDataBuilder().baseRoute(method!!)
//        assertEquals("/api", baseRoute)
    }
}