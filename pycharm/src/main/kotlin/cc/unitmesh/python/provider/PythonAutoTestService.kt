package cc.unitmesh.python.provider

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.run.PythonRunConfiguration
import com.jetbrains.python.run.PythonRunConfigurationProducer

class PythonAutoTestService : AutoTestService() {
    override fun isApplicable(element: PsiElement): Boolean = element.language.displayName == "Python"
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = PythonRunConfiguration::class.java
    override fun psiFileClass(project: Project): Class<out PsiElement> = PyFile::class.java

    override fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? {
        val psiFile: PyFile = PsiManager.getInstance(project).findFile(virtualFile) as? PyFile ?: return null
        val runManager = RunManager.getInstance(project)

        val context = ConfigurationContext(psiFile)
        val configProducer = RunConfigurationProducer.getInstance(
            PythonRunConfigurationProducer::class.java
        )
        var settings = configProducer.findExistingConfiguration(context)

        if (settings == null) {
            val fromContext = configProducer.createConfigurationFromContext(context) ?: return null
            settings = fromContext.configurationSettings
            runManager.setTemporaryConfiguration(settings)
        }
        val configuration = settings.configuration as PythonRunConfiguration
        return configuration
    }

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
        return listOf()
    }

}
