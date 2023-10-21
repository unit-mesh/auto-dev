package cc.unitmesh.devti.custom.team

import com.intellij.psi.PsiElement

class SimpleTeamContextProvider(val element: PsiElement?) : TeamContextProvider {
    /**
     * Retrieves the code of the target file associated with the given test name.
     */
    override fun underTestFileCode(methodName: String): String {
        // according to the test name, find the under test file name
        return "TODO"
    }

    override fun underTestMethodCode(testName: String): String {
        return "TODO"
    }
}