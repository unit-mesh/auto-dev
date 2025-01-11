package cc.unitmesh.go.provider.testing

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.go.context.GoMethodContextBuilder
import cc.unitmesh.go.context.GoStructContextBuilder
import cc.unitmesh.go.util.GoPsiUtil
import com.goide.GoLanguage
import com.goide.execution.testing.GoTestRunConfiguration
import com.goide.execution.testing.frameworks.gotest.GotestFramework
import com.goide.psi.*
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testIntegration.TestFinderHelper

class GoAutoTestService : AutoTestService() {
    override fun isApplicable(element: PsiElement): Boolean = element.containingFile?.language == GoLanguage.INSTANCE
    override fun isApplicable(project: Project, file: VirtualFile): Boolean {
        return file.extension == "go"
    }

    override fun runConfigurationClass(project: Project): Class<out RunProfile> = GoTestRunConfiguration::class.java
    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> = listOf()

    override fun createConfiguration(project: Project, virtualFile: VirtualFile): RunConfiguration? {
        val goFile = runReadAction { PsiManager.getInstance(project).findFile(virtualFile) as? GoFile } ?: return null

        return ConfigurationContext(goFile).configurationsFromContext?.firstOrNull {
            val configuration = it.configuration as? GoTestRunConfiguration
            configuration?.testFramework is GotestFramework
        }?.configurationSettings?.configuration
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, psiElement: PsiElement): TestFileContext? {
        val underTestElement = getElementForTests(psiElement) ?: return null
        val name = GoPsiUtil.getDeclarationName(underTestElement) ?: return null
        val testFileName = toTestFileName(name)
        val underTestFile = underTestElement.containingFile as? GoFile ?: return null

        val relatedModels = lookupRelevantClass(project, underTestElement).distinctBy { it.name }

        val imports = runReadAction {
            val importList = PsiTreeUtil.getChildrenOfTypeAsList(underTestFile, GoImportDeclaration::class.java)
            importList.map { it.text }
        }

        val currentObject = ReadAction.compute<String, Throwable> {
            return@compute when (underTestElement) {
                is GoTypeDeclaration,
                is GoTypeSpec -> {
                    GoStructContextBuilder().getClassContext(underTestElement, false)?.format()
                }

                is GoFunctionOrMethodDeclaration -> GoMethodContextBuilder().getMethodContext(
                    underTestElement,
                    false,
                    false
                )
                    ?.format()

                else -> null
            }
        }

        return TestFileContext(
            true,
            underTestFile.virtualFile,
            relatedModels,
            testFileName,
            underTestFile.language,
            currentObject,
            imports
        )
    }

    private fun toTestFileName(underTestFileName: String): String = underTestFileName + "_test.go"

    private fun getElementForTests(elementAtCaret: PsiElement): PsiElement? {
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
