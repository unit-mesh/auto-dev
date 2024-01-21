package cc.unitmesh.go.provider.testing

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.WriteTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.go.context.GoMethodContextBuilder
import cc.unitmesh.go.context.GoStructContextBuilder
import cc.unitmesh.go.util.GoPsiUtil
import com.goide.GoLanguage
import com.goide.execution.testing.GoTestRunConfiguration
import com.goide.psi.*
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testIntegration.TestFinderHelper
import com.intellij.util.PlatformUtils

class GoWriteTestService : WriteTestService() {
    override fun isApplicable(element: PsiElement): Boolean = element.containingFile?.language == GoLanguage.INSTANCE
    override fun runConfigurationClass(project: Project): Class<out RunProfile> = GoTestRunConfiguration::class.java

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        return listOf()
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val underTestElement = getElementForTests(element) ?: return null
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

    fun toTestFileName(underTestFileName: String): String {
        return underTestFileName + "_test.go"
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
