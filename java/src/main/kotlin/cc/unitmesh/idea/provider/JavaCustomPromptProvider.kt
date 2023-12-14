package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.CustomPromptProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.PsiTreeUtil

open class JavaCustomPromptProvider : CustomPromptProvider {
    /**
     * Retrieves the method under test code snippet.
     *
     * ```java
     * @Test
     * public void testFindMedian() {
     *     int[] nums1 = {1, 3};
     *     int[] nums2 = {2, 4};
     *     double expected = 2.5;
     *
     *     MathHelper mathHelper = new MathHelper();
     *     double actual = mathHelper.findMedian(nums1, nums2);
     *
     *     assertEquals(expected, actual, 0.001);
     * }
     * ```
     */
    override fun underTestMethodCode(project: Project, element: PsiElement): List<String> {
        val searchScope = GlobalSearchScope.allScope(project)

        when (element) {
            is PsiMethod -> {
                val calls: List<PsiMethodCallExpression> =
                    PsiTreeUtil.findChildrenOfAnyType(element.body, PsiMethodCallExpression::class.java)
                        .toList()

                val strings = calls
                    .mapNotNull { it ->
                        it.methodExpression.resolve()?.let {
                            it as PsiMethod
                        }
                    }
                    .filter {
                        if (it.containingClass == null) return@filter false

                        val isEmpty = ClassInheritorsSearch.search(it.containingClass!!, searchScope, true)
                            .findAll().isEmpty()

                        !isEmpty
                    }
                    .map {
                        it.text
                    }

                return strings
            }
        }

        return listOf()
    }
}
