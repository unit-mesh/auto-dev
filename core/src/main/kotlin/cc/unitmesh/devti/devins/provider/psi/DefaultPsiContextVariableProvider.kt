package cc.unitmesh.devti.devins.provider.psi

import cc.unitmesh.devti.devins.provider.PsiContextVariableProvider
import cc.unitmesh.devti.devins.variable.PsiContextVariable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class DefaultPsiContextVariableProvider : PsiContextVariableProvider {
    override fun resolve(
        variable: PsiContextVariable,
        project: Project,
        editor: Editor,
        psiElement: PsiElement?,
    ): String {
        return when (variable) {
            PsiContextVariable.FRAMEWORK_CONTEXT -> return collectFrameworkContext(psiElement, project)
            PsiContextVariable.CHANGE_COUNT -> return calculateChangeCount(psiElement)
            PsiContextVariable.LINE_COUNT -> return calculateLineCount(psiElement)
            PsiContextVariable.COMPLEXITY_COUNT -> return calculateComplexityCount(psiElement)
            PsiContextVariable.CODE_SMELL -> return CodeSmellCollector.collectElementProblemAsSting(psiElement!!, project, editor)

            else -> ""
        }
    }
}