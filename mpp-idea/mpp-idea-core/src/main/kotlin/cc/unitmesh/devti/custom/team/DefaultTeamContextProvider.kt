package cc.unitmesh.devti.custom.team

import cc.unitmesh.devti.custom.variable.MethodInputOutputVariableResolver
import cc.unitmesh.devti.custom.variable.SimilarChunkVariableResolver
import cc.unitmesh.devti.provider.CustomPromptProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.testIntegration.TestFinderHelper


/**
 * The `SimpleTeamContextProvider` class is an implementation of the `TeamContextProvider` interface.
 * It provides methods to retrieve code snippets related to the current context in a team collaboration environment.
 *
 * @property element The `PsiElement` representing the current context.
 * @property editor The `Editor` used for displaying the code snippets.
 */
class DefaultTeamContextProvider(val element: PsiElement?, val editor: Editor) : TeamContextProvider {

    /**
     * Retrieves the code snippet of the file under test that contains the specified method.
     *
     * @param methodName The name of the method.
     * @return The code snippet of the file under test that contains the specified method, or an empty string if not found.
     */
    override fun underTestFileCode(methodName: String): String {
        val psiElement = element ?: return ""
        val sourceElement = TestFinderHelper.findClassesForTest(psiElement)
        return sourceElement?.first()?.text ?: ""
    }

    /**
     * Retrieves the code snippet of the test method with the specified name.
     *
     * @param methodName The name of the test method.
     * @return The code snippet of the test method, or an empty string if not found.
     */
    override fun underTestMethodCode(methodName: String): String {
        val psiElement = element ?: return ""

        val provider = CustomPromptProvider.forLanguage(element.language)

        return provider?.underTestMethodCode(editor.project!!, psiElement)?.joinToString("\n") ?: ""
    }

    /**
     * Retrieves the similar code chunks in the current context.
     *
     * @return The similar code chunks in the current context.
     */
    override fun similarChunks(): String {
        val psiElement = element ?: return ""
        return SimilarChunkVariableResolver(psiElement).resolve()
    }

    /**
     * Retrieves the related code snippets in the current context.
     *
     * @return The related code snippets in the current context.
     */
    override fun relatedCode(): String {
        val psiElement = element ?: return ""
        return MethodInputOutputVariableResolver(psiElement).resolve()
    }
}
