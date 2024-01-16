package cc.unitmesh.go.provider.testing

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.WriteTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import com.goide.execution.testing.GoTestRunConfiguration
import com.goide.psi.GoFile
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testIntegration.TestFinderHelper
import com.intellij.util.PlatformUtils

class GoWriteTestService : WriteTestService() {
    override fun isApplicable(element: PsiElement): Boolean = PlatformUtils.isGoIde()
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = GoTestRunConfiguration::class.java

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        TODO("Not yet implemented")
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        TODO("Not yet implemented")
    }

    fun getElementForTests(elementAtCaret: PsiElement): PsiElement? {
        val parent = PsiTreeUtil.getParentOfType(elementAtCaret, GoFunctionOrMethodDeclaration::class.java, false)
        if (parent == null) {
            val goFile: GoFile = elementAtCaret as? GoFile ?: return null
            return if (goFile.functions.isNotEmpty() || goFile.methods.isNotEmpty()) {
                goFile
            } else {
                null
            }
        }

        val virtualFile = elementAtCaret.containingFile?.virtualFile ?: return null

        val project = elementAtCaret.project
        if (TestSourcesFilter.isTestSources(virtualFile, project) || TestFinderHelper.isTest(parent)) return null

        return parent
    }

}
