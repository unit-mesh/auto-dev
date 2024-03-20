package cc.unitmesh.python.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import com.intellij.execution.configurations.RunProfile
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.run.PythonRunConfiguration

class PythonAutoTestService : AutoTestService() {
    override fun isApplicable(element: PsiElement): Boolean = element.language is PythonLanguage

    override fun runConfigurationClass(project: Project): Class<out RunProfile> = PythonRunConfiguration::class.java

    fun getElementForTests(project: Project, editor: Editor): PsiElement? {
        val element = PsiUtilBase.getElementAtCaret(editor) ?: return null
        val containingFile: PsiFile = element.containingFile ?: return null

        if (InjectedLanguageManager.getInstance(project).isInjectedFragment(containingFile)) {
            return containingFile
        }

        val psiElement: PsiElement? = PsiTreeUtil.getParentOfType(
            element,
            PyFunction::class.java, false
        )
        if (psiElement != null) {
            return psiElement
        }

        return PsiTreeUtil.getParentOfType(element, PyClass::class.java, false) ?: containingFile
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        TODO("Not yet implemented")
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        TODO("Not yet implemented")
    }

}
