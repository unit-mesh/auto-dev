package cc.unitmesh.cpp.provider.testing

import cc.unitmesh.cpp.util.CppContextPrettify
import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.AutoTestService
import cc.unitmesh.devti.provider.context.TestFileContext
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration

class CppAutoTestService : AutoTestService() {
    // TODO in Cpp233 and Cpp222 the RunProfile is different, maybe we can use the same RunProfile in future
    override fun runConfigurationClass(project: Project): Class<out RunProfile>? = null

    override fun isApplicable(element: PsiElement): Boolean {
        return element.language is OCLanguage
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        // 1. check project root test folder, if not exist, create it
        val baseDir = project.guessProjectDir() ?: return null

        val sourceVirtualFile = sourceFile.virtualFile

        val testFilePath = getTestFilePath(sourceVirtualFile)
        val testFile = WriteAction.computeAndWait<VirtualFile?, Throwable> {
            baseDir.findOrCreateChildData(this, testFilePath)
        } ?: return null

        val currentClass = when (element) {
            is OCFunctionDeclaration -> {
                CppContextPrettify.printParentStructure(element)
            }

            else -> null
        }

        val relatedClasses = lookupRelevantClass(project, element)

        return TestFileContext(
            true,
            testFile,
            relatedClasses,
            "",
            sourceFile.language,
            currentClass,
            emptyList()
        )
    }

    private fun getTestFilePath(file: VirtualFile) = file.nameWithoutExtension + "_test" + "." + file.extension

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        return listOf()
    }
}
