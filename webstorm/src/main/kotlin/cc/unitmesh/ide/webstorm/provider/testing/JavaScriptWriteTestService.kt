package cc.unitmesh.ide.webstorm.provider.testing

import cc.unitmesh.devti.context.ClassContext
import cc.unitmesh.devti.provider.TestFileContext
import cc.unitmesh.devti.provider.WriteTestService
import cc.unitmesh.ide.webstorm.LanguageApplicableUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.PlatformUtils

class JavaScriptWriteTestService : WriteTestService() {
    override fun isApplicable(element: PsiElement): Boolean {
        if (PlatformUtils.isWebStorm()) return true

        val sourceFile: PsiFile = element.containingFile ?: return false
        return LanguageApplicableUtil.isJavaScriptApplicable(sourceFile.language)
    }

    override fun findOrCreateTestFile(sourceFile: PsiFile, project: Project, element: PsiElement): TestFileContext? {
        // find `__tests__` directory in root dir

        // find `__tests__` directory in current dir
        return null
    }

    override fun lookupRelevantClass(project: Project, element: PsiElement): List<ClassContext> {
        return emptyList()
    }

}
