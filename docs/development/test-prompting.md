---
layout: default
title: Test Prompts
nav_order: 2
parent: Development
---

## Test Prompts

Write unit test for following Language:
- You MUST use should_xx_xx style for test method name, You MUST use given-when-then style.
- Test file should be complete and compilable, without need for further actions.
- Ensure that each test focuses on a single use case to maintain clarity and readability.
- Instead of using `@BeforeEach` methods for setup, include all necessary code initialization within each individual test method, do not write parameterized tests.
- This project uses JUnit 4, you should import `org.junit.Test` and use `@Test` annotation.
  Kotlin API version: 1.9


// here is current class information:
'package: cc.unitmesh.idea.provider.JavaTestDataBuilder
class JavaTestDataBuilder {
+ override fun baseRoute(element: PsiElement): String
+ override fun inboundData(element: PsiElement): Map<String, String>
+ private fun handleFromType(parameter: PsiParameter): Map<@NlsSafe String, String>
+ private fun processing(returnType: PsiType): Map<@NlsSafe String, String>
+ private fun processingClassType(type: PsiClassType): Map<@NlsSafe String, String>
+ override fun outboundData(element: PsiElement): Map<String, String>
  }
  Code:
  // import cc.unitmesh.devti.provider.TestDataBuilder
  // import cc.unitmesh.idea.context.JavaContextCollection
  // import com.intellij.openapi.util.NlsSafe
  // import com.intellij.psi.*
  // import com.intellij.psi.impl.source.PsiClassReferenceType
```kotlin
/**
     * Returns the base route of a given Kotlin language method.
     *
     * This method takes a PsiElement as input and checks if it is an instance of PsiMethod. If it is not, an empty string is returned.
     * If the input element is a PsiMethod, the method checks if its containing class has the annotation "@RequestMapping" from the Spring Framework.
     * If the annotation is found, the method retrieves the value attribute of the annotation and returns it as a string.
     * If the value attribute is not a PsiLiteralExpression, an empty string is returned.
     *
     * @param element the PsiElement representing the Kotlin language method
     * @return the base route of the method as a string, or an empty string if the method does not have a base route or if the input element is not a PsiMethod
     */
    override fun baseRoute(element: PsiElement): String {
        if (element !is PsiMethod) return ""

        val containingClass = element.containingClass ?: return ""
        containingClass.annotations.forEach {
            if (it.qualifiedName == "org.springframework.web.bind.annotation.RequestMapping") {
                val value = it.findAttributeValue("value") ?: return ""
                if (value is PsiLiteralExpression) {
                    return value.value as String
                }
            }
        }

        return ""
    }
```Start  with `import` syntax here:  
