package cc.unitmesh.devti.custom.team

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.testIntegration.TestFinderHelper

class SimpleTeamContextProvider(val element: PsiElement?, val editor: Editor) : TeamContextProvider {
    /**
     * Retrieves the code of the target file associated with the given test name.
     */
    override fun underTestFileCode(methodName: String): String {
        val psiElement = element ?: return ""
        val sourceElement = TestFinderHelper.findClassesForTest(psiElement)
        return sourceElement?.first()?.text ?: ""
    }

    override fun underTestMethodCode(testName: String): String {
        return "TODO"
    }
}