package cc.unitmesh.ide.webstorm.provider.testing

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.context.TestFileContext
import cc.unitmesh.devti.provider.WriteTestService
import cc.unitmesh.ide.webstorm.LanguageApplicableUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.util.PlatformUtils


class JavaScriptWriteTestService : WriteTestService() {
    override fun isApplicable(element: PsiElement): Boolean {
        if (PlatformUtils.isWebStorm()) return true

        val sourceFile: PsiFile = element.containingFile ?: return false
        return LanguageApplicableUtil.isJavaScriptApplicable(sourceFile.language)
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        val language = sourceFile.language
        val targetFilePath = sourceFile.name.replace(".ts", ".test.ts")

        val testFile = LocalFileSystem.getInstance().findFileByPath(targetFilePath)
        if (testFile != null) {
            return TestFileContext(false, testFile, emptyList(), null, language)
        }

        val testFileName = targetFilePath.substringAfterLast("/")
        val testFileText = ""
        val testFilePsi = ReadAction.compute<PsiFile, Throwable> {
            PsiFileFactory.getInstance(project).createFileFromText(testFileName, language, testFileText)
        }

        return TestFileContext(true, testFilePsi.virtualFile, emptyList(), null, language)
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        return emptyList()
    }

}
