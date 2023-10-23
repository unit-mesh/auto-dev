package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.CustomPromptProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.childrenOfType

class JavaCustomPromptProvider : CustomPromptProvider {
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
    override fun underTestMethodCode(element: PsiElement): List<String> {
        when (element) {
            is PsiMethod -> {
                val statements = element.body?.statements ?: return emptyList()
                val calls = statements.map { it.childrenOfType<PsiMethodCallExpression>() }.flatten()
                return calls.map { it.text }
            }
        }

        return listOf()
    }
}
